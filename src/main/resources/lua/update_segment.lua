-- 更新线段树区间计数
local function update_segment()
    -- 参数检查
    if #KEYS < 1 or #ARGV < 1 then
        return redis.error_reply("Invalid number of parameters")
    end

    local segment_key = KEYS[1]  -- 线段树的key

    -- 处理所有需要更新的区间
    for i = 1, #ARGV do
        local segmentKey, delta = string.match(ARGV[i], '([^:]+):([^:]+)')
        if not segmentKey or not delta then
            return redis.error_reply("Invalid segment update format")
        end
        
        -- 使用HINCRBY命令更新计数
        local newCount = redis.call('HINCRBY', segment_key, segmentKey, tonumber(delta))
        -- 如果计数小于0，设置为0
        if newCount < 0 then
            redis.call('HSET', segment_key, segmentKey, 0)
        end
    end

    return 1
end

-- 执行更新操作并处理错误
local status, result = pcall(update_segment)
if not status then
    return redis.error_reply(result)
end
return result 