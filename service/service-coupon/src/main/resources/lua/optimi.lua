-- KEYS[1]: 优惠券信息key (c:{couponId})
-- ARGV[1]: 优惠券ID
-- ARGV[2]: 用户ID
-- ARGV[3]: 当前时间戳

-- 一次性读取所有需要的优惠券字段
local couponData = redis.call('HMGET', 'c:' .. ARGV[1],
        'id', 'status', 'startTimeStamp', 'expireTimeStamp',
        'perLimit', 'publishCount', 'segmentCount', 'receiveCount'
)

if not couponData or #couponData == 0 then
    return { -1, '优惠券不存在' }
end

-- 基础校验
local couponId, status, startTime, expireTime = couponData[1], couponData[2], tonumber(couponData[3]), tonumber(couponData[4])
local perLimit, publishCount, segmentCount, receiveCount = tonumber(couponData[5]) or 0, tonumber(couponData[6]) or -1, tonumber(couponData[7]) or 1, tonumber(couponData[8]) or 0

-- 优惠券不存在
if not couponId then
    return { -1, '优惠券不存在' }
end

-- 优惠券不可用
if status ~= '1' then
    return { -2, '优惠券不可用' }
end

-- 时间有效性检查
local now = tonumber(ARGV[3])
if now < (startTime) then
    return { -3, '活动未开始' }
end
if now >= (expireTime) then
    return { -4, '活动已结束' }
end

if publishCount > 0 and publishCount == receiveCount then
    return { -6, '库存不足' }
end

-- 用户领取key
local userKey = 'u:m:' .. ARGV[1] .. ':' .. ARGV[2]
-- 优惠券的key
local couponKey = "c:" .. ARGV[1]


-- 限领检查
if perLimit > 0 then
    local received = tonumber(redis.call('GET', userKey)) or 0
    if received >= perLimit then
        return { -5, '已达到领取限制' }
    end
end



-- 无限库存直接返回成功
if publishCount == 0 then
    redis.call('HINCRBY', couponKey, "receiveCount", 1)
    redis.call('INCR', userKey)
    redis.call('EXPIREAT', userKey, (expireTime or 0) + 86400)
    return { 1, '领取成功' }
end

-- ...前面的校验逻辑保持不变...

-- 用户ID哈希计算起始分段（新增核心优化）
local function stringHash(str)
    local hash = 5381
    for i = 1, #str do
        hash = hash * 33 + string.byte(str, i)
    end
    return hash % 2147483647  -- 2^31-1
end

local startSegment = stringHash(ARGV[2]) % segmentCount  -- 哈希取模确定起始段

-- 分段库存扣减（优化遍历逻辑）
local segKeyPrefix = 'c:s:' .. ARGV[1] .. ':'
for i = 0, segmentCount - 1 do
    -- 计算当前尝试的分段（环形遍历）
    local currentSegment = (startSegment + i) % segmentCount
    local remaining = redis.call('DECR', segKeyPrefix .. currentSegment)

    if remaining >= 0 then
        -- 记录用户领取
        redis.call('HINCRBY', couponKey, "receiveCount", 1)
        redis.call('INCR', userKey)
        redis.call('EXPIREAT', userKey, (expireTime or 0) + 86400)
        return { 1, '领取成功' }
    else
        redis.call('HINCRBY', couponKey, "receiveCount", -1)
        redis.call('INCR', segKeyPrefix .. currentSegment)  -- 回滚负库存
    end
end

return { -6, '库存不足' }
