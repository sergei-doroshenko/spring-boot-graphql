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

todo: split on 2 applications, simplify configurations and classes
? splitting in pres mode - works fine