"""有界同步执行资源：取消等待不会提前归还仍在工作的线程名额。"""

import asyncio
import contextvars
from concurrent.futures import ThreadPoolExecutor
from functools import partial
import threading


class WorkerBusy(RuntimeError):
    pass


class BoundedWorkers:
    def __init__(self, capacity: int = 4):
        self._slots = threading.BoundedSemaphore(capacity)
        self._executor = ThreadPoolExecutor(max_workers=capacity, thread_name_prefix="agent-io")

    async def run(self, fn, *args, **kwargs):
        # 不使用 executor 的无界等待队列：忙时立即拒绝。
        if not self._slots.acquire(blocking=False):
            raise WorkerBusy("同步执行资源繁忙")
        context = contextvars.copy_context()
        try:
            future = self._executor.submit(context.run, partial(fn, *args, **kwargs))
        except BaseException:
            self._slots.release()
            raise
        future.add_done_callback(lambda _: self._slots.release())
        return await asyncio.wrap_future(future)

    def shutdown(self):
        self._executor.shutdown(wait=False, cancel_futures=True)


workers = BoundedWorkers()
