# 鍓嶅悗绔仈璋?Smoke 娴嬭瘯

鐢ㄤ簬绛旇京鍓嶅揩閫熼獙璇佹牳蹇冩帴鍙ｆ槸鍚﹀彲鐢紝寤鸿姣忔澶ф敼鍚庨兘鎵ц銆?

## 1. 鑴氭湰浣嶇疆

- `scripts/smoke_test.js`锛氶€氱敤鏍稿績閾捐矾
- `scripts/smoke_contest_e2e.js`锛氱珵璧涚鍒扮锛堝缓璧涒啋鎶ュ悕鈫掓鍗曗啋鍒犻櫎娓呯悊锛?

## 2. 瑕嗙洊鑼冨洿

### 2.1 閫氱敤鏍稿績閾捐矾锛坄npm run smoke:test`锛?

1. 鐧诲綍锛歚POST /auth/login`
2. 鐢ㄦ埛淇℃伅锛歚GET /user/info`
3. 棰樼洰鍒楄〃涓庤鎯咃細`GET /problem/list`銆乣GET /problem/{id}`
4. 绔炶禌鍒楄〃涓庤鎯呬笌鎺掑悕锛歚GET /contest/list`銆乣GET /contest/{id}`銆乣GET /contest/{id}/ranking`
5. 鎻愪氦鍒楄〃锛歚GET /submission/list`
6. 璁ㄨ鍒楄〃涓庤鎯咃細`GET /discussion/list`銆乣GET /discussion/{id}`
7. 鏁欏笀/绠＄悊鍛橀澶栨鏌ワ細`GET /contest/{id}/score-snapshot`
8. 绠＄悊鍛橀澶栨鏌ワ細`GET /admin/system/configs`銆乣GET /admin/system/logs`銆乣GET /admin/system/monitor`銆乣GET /admin/system/judge-results`

### 2.2 绔炶禌绔埌绔紙`npm run smoke:contest`锛?

1. 鐧诲綍骞舵牎楠岃鑹诧紙瑕佹眰鏁欏笀鎴栫鐞嗗憳锛?
2. 鑾峰彇涓€涓鐩?ID 浣滀负寤鸿禌棰樼洰
3. 鍒涘缓鍏紑绔炶禌锛歚POST /contest`
4. 鏌ヨ绔炶禌璇︽儏锛歚GET /contest/{id}`
5. 鎶ュ悕锛歚POST /contest/{id}/join`
6. 鏌ヨ瀹炴椂姒滐細`GET /contest/{id}/ranking`
7. 鏌ヨ蹇収姒滐細`GET /contest/{id}/score-snapshot`
8. 娓呯悊鏁版嵁锛歚DELETE /contest/{id}`

## 3. 杩愯鏂瑰紡

鍦ㄧ洰褰?`onlinejudge-frontend` 鎵ц锛?

```bash
npm run smoke:test
npm run smoke:contest
```

## 4. 鐜鍙橀噺

```bash
SMOKE_BASE_URL=http://localhost:8082
SMOKE_API_PREFIX=/api
SMOKE_USERNAME=admin2
SMOKE_PASSWORD=admin123
SMOKE_TIMEOUT_MS=15000
```

PowerShell 绀轰緥锛?

```powershell
$env:SMOKE_BASE_URL="http://localhost:8082"
$env:SMOKE_API_PREFIX="/api"
$env:SMOKE_USERNAME="admin2"
$env:SMOKE_PASSWORD="admin123"
npm run smoke:test
npm run smoke:contest
```

## 5. 缁撴灉鍒ゅ畾

- 鍏ㄩ儴姝ラ `OK` 涓旀渶缁?`failed: 0` 瑙嗕负閫氳繃銆?
- 浠讳竴姝ラ澶辫触锛岃剼鏈繑鍥為潪 0锛屽彲鐩存帴鐢ㄤ簬 CI 鎴栧彂甯冨墠妫€鏌ャ€?

