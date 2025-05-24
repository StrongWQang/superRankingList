-- 检查请求ID是否存在，如果不存在则执行更新操作
local function update_ranking()
    -- 参数检查
    if #KEYS < 2 or #ARGV < 2 then
        return redis.error_reply("参数数量不正确")
    end

    local ranking_key = KEYS[1]
    local user_id = KEYS[2]
    local request_id = ARGV[1]
    local score = tonumber(ARGV[2])

    if not score then
        return redis.error_reply("分数必须是数字")
    end

    -- 检查请求ID是否存在
    local res = redis.call('SET', ranking_key .. '_' .. request_id, '0', 'EX', '3600', 'NX')
    
    -- 如果请求ID不存在，则执行更新操作
    if res ~= false then
        redis.call('ZINCRBY', ranking_key, score, user_id)
        return 1
    end
    
    return 0
end

-- 执行更新操作并处理错误
local status, result = pcall(update_ranking)
if not status then
    return redis.error_reply(result)
end
return result 