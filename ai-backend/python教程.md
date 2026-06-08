# Python 快速教程（Java 程序员视角）

> 目标：能熟练编写 `ai4.py` 这类 LangChain Agent 代码。

---

## 一、语法速览 — 和 Java 的关键差异

### 1. 没有分号，缩进即代码块

```python
# Python：缩进决定作用域
if x > 0:
    print("正数")      # 这行属于 if
    y = x * 2          # 这行也属于 if
print("done")          # 这行不属于 if，缩进回来了

// Java：花括号决定作用域
if (x > 0) {
    System.out.println("正数");
    int y = x * 2;
}
System.out.println("done");
```

### 2. 动态类型 + 类型标注（可选）

```python
# Python：变量不需要声明类型，但可以标注（推荐）
name: str = "hello"           # 类型标注，IDE 会检查
count: int = 0
items: list[str] = ["a", "b"]  # 等价于 Java 的 List<String>

# 函数签名
def greet(name: str) -> str:        # Java: String greet(String name)
    return f"Hello, {name}"

def process(data: dict) -> list:    # Java: List process(Map data)
    ...
```

### 3. None 不是 null

```python
# Python 的 None ≈ Java 的 null
result = None
if result is None:        # 用 is，不要用 ==
    print("空值")
if result is not None:    # 非空判断
    print("有值")
```

### 4. 布尔值和条件

```python
# Python          # Java
True               true
False              false
None               null
not x              !x
x and y            x && y
x or y             x || y
```

---

## 二、函数定义 — 对应 Java 方法

### 普通函数

```python
# Python                              // Java
def add(a: int, b: int) -> int:       int add(int a, int b) {
    return a + b                           return a + b;
                                      }
```

### 默认参数

```python
def greet(name: str = "世界") -> str:
    return f"你好，{name}"

greet()         # "你好，世界"
greet("张三")   # "你好，张三"
```

### async 函数（协程）

```python
# Python 的 async/await ≈ Java 的 CompletableFuture / 虚拟线程
# 规则：async def 定义协程，await 等待结果，不阻塞线程

async def fetch_data() -> dict:
    result = await some_io_operation()   # 等待 IO，不占 CPU
    return result

# 只能在 async 函数里用 await
# 普通函数里不能写 await
```

**和 Java 的对应关系：**

| Python | Java |
|--------|------|
| `async def` | `CompletableFuture<T>` 返回类型 |
| `await` | `.get()` 或 `thenApply()` |
| `asyncio.Queue` | `BlockingQueue` |
| `asyncio.create_task()` | `CompletableFuture.supplyAsync()` |

---

## 三、类 — 对应 Java class

### 普通类

```python
# Python                              // Java
class Person:                         public class Person {
    def __init__(self, name: str,         public Person(String name, int age) {
                 age: int):                   this.name = name;
        self.name = name                    this.age = age;
        self.age = age                  }
                                      }
    def greet(self) -> str:            public String greet() {
        return f"我是{self.name}"           return "我是" + this.name;
                                      }
# 实例化                              // 实例化
p = Person("张三", 25)                 Person p = new Person("张三", 25);
```

**关键差异：**
- `__init__` = 构造函数（双下划线开头/结尾 = 特殊方法）
- `self` = `this`，**必须显式写在方法第一个参数**
- 没有 `private`/`public`，约定 `_name` 前缀表示"别碰"

### Pydantic 模型 — 对应 Java POJO / Request Body

```python
from pydantic import BaseModel, Field

# 相当于 Java 的 @Data class + @NotNull 校验
class ChatRequest(BaseModel):
    question: str = Field(..., description="用户问题")
    thread_id: str = Field(default_factory=lambda: str(uuid.uuid4())[:8])
    #                                ↑ 默认值工厂，相当于 Java Supplier<String>

# 使用
req = ChatRequest(question="你好")
print(req.question)     # "你好"
req.model_dump()        # 转为 dict：{"question": "你好", "thread_id": "abc12345"}
```

---

## 四、装饰器 — 对应 Java 注解

```python
# Python 装饰器 ≈ Java 注解，本质是函数包装器

# FastAPI 路由（等价于 Spring @GetMapping）
@app.get("/api/users")
def get_users():
    return [{"name": "张三"}]

# Java 等价写法：
# @GetMapping("/api/users")
# public List<User> getUsers() { ... }

# 常用装饰器：
@app.get("/path")        # GET 请求
@app.post("/path")       # POST 请求
@app.delete("/path/{id}") # DELETE 请求，{id} = @PathVariable
```

---

## 五、字符串 — f-string 无敌好用

```python
name = "张三"
age = 25

# Python f-string（最常用）
s = f"我叫{name}，今年{age}岁"

# 等价 Java String.format
# String s = String.format("我叫%s，今年%d岁", name, age);

# 多行字符串（三引号）
sql = """
    SELECT *
    FROM users
    WHERE name = ?
"""
```

---

## 六、数据结构 — 对标 Java 集合

| Python | Java | 示例 |
|--------|------|------|
| `list` | `ArrayList` / `List` | `[1, 2, 3]` |
| `dict` | `HashMap` / `Map` | `{"key": "value"}` |
| `tuple` | 不可变 List | `(1, 2)` |
| `set` | `HashSet` | `{1, 2, 3}` |

```python
# list 操作
items = [1, 2, 3]
items.append(4)         # add()
items[0]                # get(0)
len(items)              # size()
items[-1]               # 最后一个元素（负索引，没有直接的 Java 对应）

# dict 操作
d = {"a": 1, "b": 2}
d["a"]                  # get("a")
d.get("c", 0)           # getOrDefault("c", 0)
d.keys()                # keySet()
d.values()              # values()

# 遍历
for k, v in d.items():  # for (Entry<String,Integer> e : map.entrySet())
    print(k, v)
```

### 列表推导式 — 对应 Java Stream

```python
# Python 列表推导式
names = [u.name for u in users if u.age > 18]
# 等价 Java：users.stream().filter(u -> u.age > 18).map(u -> u.name).toList()

# dict 推导式
d = {r["id"]: r["name"] for r in rows}
# 等价：rows.stream().collect(toMap(r -> r.id, r -> r.name))
```

---

## 七、异常处理 — try/except/finally

```python
# Python                              // Java
try:                                  try {
    result = risky_operation()            result = riskyOperation();
    conn.execute(sql)                     conn.execute(sql);
except ValueError as e:               } catch (ValueError e) {
    print(f"值错误: {e}")                 System.out.println("值错误: " + e);
except Exception as e:                } catch (Exception e) {
    print(f"出错了: {e}")                 System.out.println("出错了: " + e);
else:                                 } // Python 独有：无异常时执行
    print("没出错")
finally:                              } finally {
    conn.close()                          conn.close();
                                      }
```

**重点：Python 用 `try/finally` 模式管理资源（Java 用 try-with-resources）**

```python
conn = get_connection()
try:
    do_stuff(conn)
finally:
    conn.close()          # 无论如何都会执行

# 对于支持 with 的对象，可以用更简洁的写法（见下一节）
```

---

## 八、with 语句 — 对应 Java try-with-resources

```python
# Python with = Java try-with-resources 的进化版

# Java:
# try (Connection conn = getConnection()) {
#     doStuff(conn);
# }  // 自动 close

# Python:
checkpointer_ctx = SqliteSaver.from_conn_string(DB_PATH)
checkpointer = checkpointer_ctx.__enter__()   # 手动进入
try:
    do_stuff(checkpointer)
finally:
    checkpointer_ctx.__exit__(None, None, None)  # 手动退出

# 等价简写（如果对象实现了 __enter__/__exit__）：
# with SqliteSaver.from_conn_string(DB_PATH) as checkpointer:
#     do_stuff(checkpointer)
```

---

## 九、线程与协程 — ai4.py 的核心模式

ai4.py 的关键：**async 主循环 + 同步工作线程 + Queue 桥接**

```python
import asyncio

# 1. 创建跨线程队列（≈ Java BlockingQueue）
queue: asyncio.Queue = asyncio.Queue()

# 2. 同步工作函数（将在线程中运行）
def worker():
    for i in range(5):
        queue.put_nowait(f"数据{i}")   # 线程安全入队
    queue.put_nowait(None)              # 结束信号

# 3. 异步消费者
async def consumer():
    while True:
        data = await queue.get()        # 不阻塞，异步等待
        if data is None:
            break
        print(data)

# 4. 启动：把同步 worker 丢进线程池
from concurrent.futures import ThreadPoolExecutor
executor = ThreadPoolExecutor(max_workers=1)
executor.submit(worker)                 # 线程池启动 worker

# 5. 运行异步消费者
asyncio.run(consumer())
```

**关键点：`loop.call_soon_threadsafe(queue.put_nowait, data)`**
- 从子线程往主协程的队列投数据，确保线程安全
- 相当于 Java 的 `Platform.runLater(() -> queue.put(data))`

---

## 十、生成器（yield）— SSE 流式的关键

```python
# 生成器：函数里用 yield 代替 return，逐个产出值
# ≈ Java 的 Iterator，但语法更简洁

def count_up_to(n: int):
    """生成器函数"""
    for i in range(1, n + 1):
        yield i          # 产出值，函数暂停，下次 next() 继续

# 使用
for num in count_up_to(5):
    print(num)           # 1 2 3 4 5

# 在 ai4.py 中，SSE 流式响应就是靠 yield：
async def _stream_chat(question, thread_id):
    while True:
        data = await queue.get()
        if data is None:
            yield "data: [DONE]\n\n"    # 推送给前端
            break
        yield f"data: {data}\n\n"       # 逐个 token 推送
```

---

## 十一、常用代码片段（对照 Java）

### 数据库操作模式

```python
# Python 模式：get → try → finally close
def query_users(name: str) -> list[dict]:
    conn = get_app_db()
    try:
        rows = conn.execute(
            "SELECT * FROM users WHERE name = ?", (name,)
        ).fetchall()
        return [dict(r) for r in rows]    # Row → dict
    finally:
        conn.close()                       # 确保关闭

# Java 对比：
# try (Connection conn = getConnection()) {
#     PreparedStatement ps = conn.prepareStatement("SELECT * FROM users WHERE name = ?");
#     ps.setString(1, name);
#     ResultSet rs = ps.executeQuery();
#     // 手动映射...
# }
```

### 远程调用 + 异常兜底

```python
try:
    res = await axios_get(url)
    return res["data"]
except Exception:
    return None              # 兜底返回（类似 Java Optional.empty()）
```

### 字符串截取做标题

```python
title = text[:30] + ("..." if len(text) > 30 else "")
# 等价 Java：
# String title = text.length() > 30 ? text.substring(0, 30) + "..." : text;
```

### 当前时间字符串

```python
from datetime import datetime
now = datetime.now().isoformat()   # "2026-05-31T14:30:00.123456"
```

---

## 十二、常见坑

| 问题 | 错误写法 | 正确写法 |
|------|---------|---------|
| None 判断 | `if x == None` | `if x is None` |
| 空列表默认参数 | `def f(items=[])` | `def f(items=None): if items is None: items = []` |
| 字符串拼接非 str | `"答案:" + 42` | `f"答案:{42}"` |
| 多线程修改 list | 直接 append | 用 `queue.put_nowait()` 跨线程通信 |
| 闭包变量 | 内层函数直接赋值 | 用 `nonlocal` 声明，或操作可变对象 |
| dict 取值可能不存在 | `d["key"]` | `d.get("key", default)` |

---

## 十三、速查表

```python
# ---- 你写 Agent 代码最常用的 20 个语法 ----

# 1. 导入
from xxx import ClassName

# 2. 函数
def fn(arg: str) -> str: ...

# 3. 异步函数
async def fn(): ...

# 4. 装饰器（注解）
@app.get("/path")

# 5. f-string
f"值={value}"

# 6. 列表推导
[d["key"] for d in dict_list]

# 7. 条件表达式
x if condition else y

# 8. None 检查
if x is None: ...
if x is not None: ...

# 9. 字典
{"key": value, **other_dict}

# 10. try/finally（资源释放）
try: ...
finally: conn.close()

# 11. 生成器（yield）
yield value

# 12. 类型标注
items: list[str] = []

# 13. 默认参数
def fn(name: str = "默认"): ...

# 14. 遍历
for item in items: ...
for i, item in enumerate(items): ...  # 带索引
for k, v in d.items(): ...            # 字典

# 15. lambda（单行匿名函数）
lambda x: x * 2

# 16. Pydantic 模型
class Req(BaseModel):
    field: str = Field(...)

# 17. sqlite3 参数化查询
conn.execute("SELECT * FROM t WHERE id = ?", (id,))

# 18. uuid
import uuid
str(uuid.uuid4())[:8]   # "a1b2c3d4"

# 19. os.path
os.path.join(dir, "file.db")
os.path.dirname(__file__)

# 20. datetime
from datetime import datetime
datetime.now().isoformat()
```

---

## 十四、继续学习

1. **官方文档**：https://docs.python.org/zh-cn/3/tutorial/
2. **FastAPI 文档**：https://fastapi.tiangolo.com/zh/
3. **LangChain 文档**：https://python.langchain.com/docs/introduction/
4. **对着 `ai4.py` 逐行看**：本教程每个语法点都能在 ai4.py 里找到实际用例，建议把它当成参考范例来读。