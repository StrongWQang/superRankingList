-- 更新排行榜积分
local function update_ranking()
    -- 参数检查
    if #KEYS < 2 or #ARGV < 2 then
        return redis.error_reply("Invalid number of parameters")
    end

    local ranking_key = KEYS[1]
    local user_id = tostring(KEYS[2])  -- 确保user_id是字符串格式
    local score = tonumber(ARGV[1])    -- 确保score是数字
    local timestamp = ARGV[2]

    if not score then
        return redis.error_reply("Score must be a number")
    end

    if not timestamp then
        return redis.error_reply("Timestamp is required")
    end

    -- 获取用户的当前积分
    local current_score = redis.call('ZSCORE', ranking_key, user_id)
    local integer = 0  -- 整数部分。如果用户不存在，则其为0
    
    if current_score ~= false then
        integer = math.floor(current_score)  -- 如果用户存在，则截取整数部分
    end
    
    integer = integer + score  -- 更新整数部分，即更新积分
    
    -- 将时间戳转换为四位小数
    local timestamp_str = tostring(timestamp)
    if #timestamp_str < 4 then
        return redis.error_reply("Invalid timestamp format")
    end
    
    local last_four = string.sub(timestamp_str, -4)  -- 获取最后四位
    local timestamp_decimal = tonumber('0.' .. last_four)  -- 转换为小数
    
    if not timestamp_decimal then
        return redis.error_reply("Failed to convert timestamp to decimal")
    end
    
    local final_score = integer + timestamp_decimal  -- 将整数部分与小数部分组合为浮点数
    
    redis.call('ZADD', ranking_key, final_score, user_id)  -- 使用 ZADD 重置用户积分
    return 1
end

-- 执行更新操作并处理错误
local status, result = pcall(update_ranking)
if not status then
    return redis.error_reply(result)
end
return result 