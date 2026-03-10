# 判题沙箱说明（Docker）

## 1. 功能

后端已支持 Docker 沙箱判题，默认关闭。  
开启后，JAVA/CPP/PYTHON 判题会优先在容器内执行，并启用以下限制：

- `--network none`（禁网）
- `--read-only`（容器根文件系统只读）
- `--cap-drop ALL` + `no-new-privileges`
- `--memory`（内存限制）
- `--cpus`（CPU 配额）
- `--pids-limit`（进程数量限制）
- `--tmpfs /tmp`（临时目录）

若开启 `strict=false`，沙箱失败会自动回退到本地判题；`strict=true` 则直接判题失败。

## 2. 配置

`application.yml`:

```yaml
judge:
  sandbox:
    enabled: true
    strict: false
    docker-binary: docker
    java-image: eclipse-temurin:17-jdk
    cpp-image: gcc:13
    python-image: python:3.11-alpine
    cpus: "1.0"
    pids-limit: 128
    tmpfs-size: 64m
    compile-timeout: 15000
```

## 3. 运行前准备

1. 本机安装并启动 Docker。
2. 预拉取镜像（可选，加快首个提交）：
   - `docker pull eclipse-temurin:17-jdk`
   - `docker pull gcc:13`
   - `docker pull python:3.11-alpine`

