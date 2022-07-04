function waitForController {
  while [[ "$(curl -w "%{http_code}" -m 1 -s -k -o /dev/null https://127.0.0.1:1280/version)" != "200" ]]; do
    echo "waiting for https://127.0.0.1:1280"
    sleep 3
  done
}

echo Starting Ziti network
docker-compose -f simplified-docker-compose.yml -f docker-compose.yml up -d ziti-edge-router ziti-controller-init-container postgres-db

echo Waiting for network to initialize
waitForController


ziti edge -i spring-jpa login -y -i spring-jpa 127.0.0.1:1280 -u admin -p admin

#if !ziti edge -i spring-jpa list edge-routers > /dev/null 2>&1; then
#	echo "Error: Log into OpenZiti before running this script" 
#	exit 1
#fi

rm private-service.jwt client.jwt database.jwt private-service.json client.json database.json 2> /dev/null

echo Creating identities
ziti edge -i spring-jpa create identity device private-service -o private-service.jwt -a "services"
ziti edge -i spring-jpa create identity device client -o client.jwt -a "clients"
ziti edge -i spring-jpa create identity device database -o database.jwt -a "databases"

echo Enrolling identities
ziti edge enroll -j private-service.jwt
ziti edge enroll -j client.jwt
ziti edge enroll -j database.jwt

echo Creating demo-service
ziti edge -i spring-jpa create config demo-service-config ziti-tunneler-client.v1 '{"hostname": "example.web","port": 8080}'
ziti edge -i spring-jpa create service demo-service --configs demo-service-config -a "demo-service"

echo Creating database service
ziti edge -i spring-jpa create config private-postgres-intercept.v1 intercept.v1 '{"protocols":["tcp"],"addresses":["private-postgres-server.demo"], "portRanges":[{"low":5432, "high":5432}]}'
ziti edge -i spring-jpa create config private-postgres-host.v1 host.v1 '{"protocol":"tcp", "address":"private-postgres-db","port":5432 }'
ziti edge -i spring-jpa create service private-postgres --configs private-postgres-intercept.v1,private-postgres-host.v1 -a "private-postgres-services"

echo Creating identity service policies
ziti edge -i spring-jpa create service-policy service-bind-policy Bind --identity-roles "#services" --service-roles "#demo-service"
ziti edge -i spring-jpa create service-policy service-dial-policy Dial --identity-roles "#clients" --service-roles "#demo-service"
ziti edge -i spring-jpa create service-policy database-bind-policy Bind --identity-roles "#databases" --service-roles "#private-postgres-services"
ziti edge -i spring-jpa create service-policy database-dial-policy Dial --identity-roles "#services" --service-roles "#private-postgres-services"

echo Starting database tunneler
docker-compose -f simplified-docker-compose.yml -f docker-compose.yml up -d ziti-tunneler

echo The demo Ziti network is up and running
echo TODO: Enumerate what is available
