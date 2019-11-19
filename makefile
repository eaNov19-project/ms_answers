DOCKERHUBREPO=islamahmad
IMAGE=${DOCKERHUBREPO}/eaproj-answerms:1.0.4

# ===== Maven =====
maven-rebuild:
	mvn clean && mvn install

# ===== Docker =====
docker-build: maven-rebuild
	docker build -t ${IMAGE} .

docker-run:
	docker run -p 8080:8092 ${IMAGE}

docker-login:
	docker login

docker-push: docker-login docker-build
	docker push ${IMAGE}

k8-install:
	kubectl apply -f k8s-deploy.yaml

k8-delete:
	kubectl delete -f k8s-deploy.yaml

k8-restart: k8-delete k8-install

k8-repush-restart: k8-delete docker-push k8-install



build:
	 mvn clean && mvn install && docker build -t ${IMAGE} . && docker push $(IMAGE)

config:
	kubectl apply -f k8s-config.yaml

deploy:
	kubectl apply -f k8s-deploy.yaml