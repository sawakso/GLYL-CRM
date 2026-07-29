# 5174 CRM 协作说明

本分支是独立的纷享销客风格 CRM 定制版本，服务端口固定为 `5174`，数据库、Redis、容器、网络和数据卷均不与原 Cordys CRM 共用。

## 第一次拉取

```powershell
git clone https://github.com/LianXiangShi/fxiaoke-crm-5174.git
cd fxiaoke-crm-5174
Copy-Item installer/.env.example installer/.env
```

打开 `installer/.env`，将所有 `CHANGE_ME` 替换为团队自己的强密码和随机密钥，然后启动：

```powershell
docker compose -f installer/docker-compose.fxiaoke.yml up -d --build
```

访问地址：<http://localhost:5174>

## 日常协作

```powershell
git pull --rebase
git switch -c feature/<功能名称>
```

完成修改后提交到自己的功能分支并发起合并请求，不要直接向上游 CordysCRM 的 `main` 分支推送。

## 常用检查

```powershell
docker compose -f installer/docker-compose.fxiaoke.yml ps
docker compose -f installer/docker-compose.fxiaoke.yml logs --tail=200
```

停止服务时不要附加 `-v`，否则会删除专属数据卷：

```powershell
docker compose -f installer/docker-compose.fxiaoke.yml down
```

独立资源名称：

- MySQL 数据库：`fxiaoke_crm_5174`
- 容器：`fxiaoke-crm-5174`
- 数据卷：`fxiaoke-crm-5174-data`
- 网络：`fxiaoke-crm-5174-network`
