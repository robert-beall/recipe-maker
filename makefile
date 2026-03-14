index:
	- @echo "run: build and start the project containers"
	- @echo "down: teardown the project containers"
	- @echo "wipe: teardown the project containers and remove volumes"

run:
	@docker-compose -f docker-compose.yml up -d
down: 
	@docker-compose -f docker-compose.yml down
wipe:
	@docker-compose -f docker-compose.yml down -v