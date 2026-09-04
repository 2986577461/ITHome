-- KEYS[1]~KEYS[8]：详情 Hash 和 7 个正式排名 ZSET
for index = 1, 8 do
    redis.call('DEL', KEYS[index])
end

-- KEYS[9]：缓存构建完成标记
redis.call('DEL', KEYS[9])

-- KEYS[10]~KEYS[17]：对应的临时详情 Hash 和临时排名 ZSET
for index = 10, 17 do
    if redis.call('EXISTS', KEYS[index]) == 1 then
        redis.call('RENAME', KEYS[index], KEYS[index - 9])
    end
end

-- 所有正式 key 已切换完成，最后写入 ready 标记
redis.call('SET', KEYS[9], '1', 'EX', ARGV[1])
return 1
