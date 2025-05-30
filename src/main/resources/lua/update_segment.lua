-- 更新线段树区间计数
local function update_segment()
    -- 参数检查
    if #KEYS < 1 or #ARGV < 1 then
        return redis.error_reply("Invalid number of parameters")
    end

    local segment_key = KEYS[1]  -- 线段树的key

    -- 处理所有需要更新的区间
    for i = 1, #ARGV do
        local segment_info = ARGV[i]
        -- 移除可能存在的引号
        segment_info = string.gsub(segment_info, '"', '')
        -- 使用更宽松的模式匹配，支持小数点格式
        local segmentKey, delta = string.match(segment_info, "([%d%.]+%-[%d%.]+):([-]?%d+)")
        
        if not segmentKey or not delta then
            return redis.error_reply("Invalid segment update format: " .. tostring(segment_info))
        end
        
        -- 确保delta是整数
        delta = tonumber(delta)
        if not delta then
            return redis.error_reply("Invalid delta value: " .. tostring(delta))
        end
        
        -- 更新计数
        local newCount = redis.call('HINCRBY', segment_key, segmentKey, delta)
        -- 如果计数小于0，设置为0
        if newCount < 0 then
            redis.call('HSET', segment_key, segmentKey, 0)
        end
    end

    return 1
end

return update_segment() 