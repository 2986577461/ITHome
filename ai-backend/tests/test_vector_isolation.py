import json
from unittest.mock import Mock

import pytest

from common.vector_store import VectorStore


@pytest.fixture
def store():
    store = VectorStore.__new__(VectorStore)
    store.client = Mock()
    store.embed_one = Mock(return_value=[1.0])
    store.embed = Mock(return_value=[[1.0]])
    store.client.search.return_value = [[]]
    store.client.query_iterator.return_value.next.side_effect = [[]]
    return store


@pytest.mark.parametrize("user_id", ["", " ", None, "a" * 129])
@pytest.mark.parametrize("operation", ["search", "read", "delete", "delete_user", "insert"])
def test_invalid_identity_fails_closed(store, user_id, operation):
    with pytest.raises(ValueError):
        if operation == "search":
            store.search("question", user_id)
        elif operation == "read":
            store.get_chunks_by_doc(1, user_id, owner_id="alice")
        elif operation == "delete":
            store.delete_document(1, user_id)
        elif operation == "delete_user":
            store.delete_documents_by_user(user_id)
        else:
            store.insert_chunks(1, ["text"], user_id)
    assert not store.client.mock_calls
    store.embed_one.assert_not_called()
    store.embed.assert_not_called()


@pytest.mark.parametrize("doc_id", [0, -1, True, "1 or doc_id > 0"])
def test_invalid_document_id_cannot_change_filter(store, doc_id):
    with pytest.raises(ValueError):
        store.delete_document(doc_id, "alice")
    store.client.delete.assert_not_called()


def test_shared_read_search_and_owner_scoped_delete(store):
    store.search("question", "bob")
    assert "filter" not in store.client.search.call_args.kwargs
    store.get_chunks_by_doc(12, "bob", owner_id="alice")
    assert store.client.query_iterator.call_args.kwargs["filter"] == 'doc_id == 12 and user_id == "alice"'
    store.delete_document(12, "alice")
    assert store.client.delete.call_args.kwargs["filter"] == 'doc_id == 12 and user_id == "alice"'


def test_user_filter_escapes_quotes_and_backslashes(store):
    user = 'alice" or user_id != "\\'
    store.delete_document(1, user)
    expr = store.client.delete.call_args.kwargs["filter"].split(" and user_id == ", 1)[1]
    assert json.loads(expr) == user


def test_chunk_iterator_collects_all_batches_and_closes(store):
    iterator = store.client.query_iterator.return_value
    iterator.next.side_effect = [
        [{"chunk_idx": 1, "content": "second"}],
        [{"chunk_idx": 0, "content": "first"}],
        [],
    ]
    assert store.get_chunks_by_doc(1, "alice", owner_id="alice") == [
        {"chunk_idx": 0, "content": "first"}, {"chunk_idx": 1, "content": "second"},
    ]
    iterator.close.assert_called_once()


def test_chunk_iterator_closes_on_error(store):
    iterator = store.client.query_iterator.return_value
    iterator.next.side_effect = RuntimeError("offline")
    with pytest.raises(RuntimeError):
        store.get_chunks_by_doc(1, "alice", owner_id="alice")
    iterator.close.assert_called_once()
