"""Local, explicit-label retrieval benchmark. Never writes corpus or labels into the repo."""
import argparse
import json
import sys
import tempfile
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from common.vector_store import VectorStore
from kb.documents import chunk_text, extract_text


def evaluate(manifest, db_path):
    store = VectorStore(str(db_path))
    user = "benchmark"
    documents = {}
    for index, doc in enumerate(manifest["documents"], 1):
        path = Path(doc["path"])
        ext = path.suffix.lower().lstrip(".")
        text = extract_text(path.read_bytes(), ext)
        chunks = chunk_text(text, ext)
        store.insert_chunks(index, chunks, user)
        documents[doc["id"]] = (index, text, chunks)
    results = []
    for case in manifest["queries"]:
        doc_id, text, chunks = documents[case["document"]]
        evidence = case["evidence"]
        if not evidence or evidence not in text:
            raise ValueError(f"evidence is absent in extracted document for {case['id']}")
        hits = store.search(case["query"], user_id=user, top_k=8)
        selected = [h for h in hits if h["score"] >= 0.5]
        matched = lambda h: h["doc_id"] == doc_id and evidence in h["content"]
        relevant = [(i + 1, float(h["score"])) for i, h in enumerate(hits) if matched(h)]
        results.append({"id": case["id"], "format": Path(next(d['path'] for d in manifest['documents'] if d['id'] == case['document'])).suffix,
                        "evidence_in_chunk": any(evidence in c for c in chunks),
                        "hit_at_8": bool(relevant),
                        "rank": relevant[0][0] if relevant else None,
                        "score": round(relevant[0][1], 4) if relevant else None,
                        "selected_at_8": any(matched(h) for h in selected)})
    return results


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("manifest", type=Path)
    args = parser.parse_args()
    manifest = json.loads(args.manifest.read_text(encoding="utf-8"))
    with tempfile.TemporaryDirectory() as directory:
        results = evaluate(manifest, Path(directory) / "milvus.db")
    for r in results:
        print(json.dumps(r, ensure_ascii=False))
    if not results:
        raise ValueError("no benchmark queries")
    print(f"Hit@8: {sum(r['hit_at_8'] for r in results)}/{len(results)}; "
          f"selected@8 (score >= 0.5): {sum(r['selected_at_8'] for r in results)}/{len(results)}")


if __name__ == "__main__":
    main()
