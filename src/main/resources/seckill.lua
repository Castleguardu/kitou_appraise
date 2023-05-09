--1.1优惠券id

local voucherId = ARGV[1]
local userId = ARGV[2]
local orderId = ARGV[3]

local stockKey = 'seckill:stock:'..voucherId
local stockKey = 'seckill:stock:'..voucherId

--业务
--判断库存是否充足 get stockkey
if (tonumber(redis.call('get',stockKey))<=0)
then
     return 1
end
if (tonumber(redis.call('sismember',orderKey,userId))==1) then
    return 2
end
redis.call('incrby',stockKey,-1)
redis.call('sadd',orderKey,userId)

redis.call('xadd','stream.orders','*','userId',userId,'voucherId',voucherId,'id',id)
return 0