import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { test } from "node:test";

function interceptors(token) {
  const callbacks = {};
  const instance = {
    interceptors: {
      response: { use: (success, failure) => { callbacks.failure = failure; } },
      request: { use: (handler) => { callbacks.request = handler; } },
    },
  };
  const messages = [];
  const source = readFileSync(new URL("../src/request/axiosInit.js", import.meta.url), "utf8")
    .replace(/^import .*;\r?\n/gm, "")
    .replace("export default axiosInstance;", "return axiosInstance;");
  new Function("axios", "ElMessage", "localStorage", source)(
    { create: () => instance },
    { error: (message) => messages.push(message) },
    { getItem: () => token },
  );
  return { ...callbacks, messages };
}

test("REST requests attach the current login token", () => {
  const handlers = interceptors("current-token");
  assert.equal(handlers.request({ headers: {} }).headers.Authorization, "current-token");
});

test("401 is rejected instead of being converted into successful undefined", async () => {
  const handlers = interceptors("current-token");
  const error = { response: { status: 401 } };
  await assert.rejects(handlers.failure(error), (rejected) => rejected === error);
  assert.match(handlers.messages[0], /重新登录/);
});

test("503 shows dependency failure detail, not a rate limit message", async () => {
  const handlers = interceptors("current-token");
  const error = { response: { status: 503, data: { detail: "无法连接身份验证服务" } } };
  await assert.rejects(handlers.failure(error), (rejected) => rejected === error);
  assert.equal(handlers.messages[0], "无法连接身份验证服务");
});
