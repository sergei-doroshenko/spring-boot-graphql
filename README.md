http://localhost:8080/h2-console/login.do

http://localhost:8080/graphql/schema.json

http://localhost:8080/graphql?query={findAllCars{id vin make}}
http://localhost:8080/graphql?query=%7BfindAllCars%7Bid%20vin%20make%7D%7D

postman POST
`{
	"query":"{findAllCars{id vin make}}"
}`

for subscription:
`ws://localhost:8080/messages-spqr`
query
`
{
	"query": "subscription {messages {id}}",
	"variables": null
}
`

logs without and with batch fetching:
...
```
Hibernate: 
    /* select
        generatedAlias0 
    from
        Message as generatedAlias0 */ select
            message0_.id as id1_2_,
            message0_.body as body2_2_,
            message0_.car_id as car_id3_2_,
            message0_.conversation_id as conversa4_2_ 
        from
            message message0_
```
```
2018-04-05 17:30:53.133 DEBUG 6168 --- [nio-9000-exec-1] o.s.jdbc.core.JdbcTemplate               : Executing SQL query [select * from car where id =1]
2018-04-05 17:30:53.159 DEBUG 6168 --- [nio-9000-exec-1] o.s.jdbc.core.JdbcTemplate               : Executing SQL query [select * from car where id =2]

```            
```
Hibernate: 
   /* select
       generatedAlias0 
   from
       Message as generatedAlias0 */ select
           message0_.id as id1_2_,
           message0_.body as body2_2_,
           message0_.car_id as car_id3_2_,
           message0_.conversation_id as conversa4_2_ 
       from
           message message0_
```

```
2018-04-05 18:27:17.883 DEBUG 15196 --- [nio-9000-exec-4] o.s.jdbc.core.JdbcTemplate               : Executing SQL query [select * from car where id in(1,2)]
```
