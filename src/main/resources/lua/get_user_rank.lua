-- 获取用户排名和积分的Lua脚本
-- KEYS[1]: 排行榜key
-- KEYS[2]: 用户ID
local rank = redis.call('ZREVRANK', KEYS[1], KEYS[2])
local score = redis.call('ZSCORE', KEYS[1], KEYS[2])
if rank == false then
    return {-1, 0}
else
    return {rank, score}
end 