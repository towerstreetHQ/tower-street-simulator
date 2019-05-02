# Tower Street Simulator

## Setup

### Requirements

* SBT 1.0
* Yarn
* JVM
* Postgres 10

### Db setup

1) Create empty database and user with password

2) Create secret config file in project `backend/api/attacksimulator-api`.

File name:
```
backend/api/attacksimulator-api/src/main/resources/secret.conf
```

Content:
```
slick.dbs.default.db.user = "towerstreet"
slick.dbs.default.db.password = ...
slick.dbs.default.db.url = "jdbc:postgresql://127.0.0.1/towerstreet"
```

3) Run simulator backend. Database will be populated by flyway migrations.

## Run on local

### API

Go to root folder.

Run local server:
```
sbt attacksimulatorApi/run
```

Server will be listening on localhost port 9001:
```
curl localhost:9001/version
```

### APP

Go to `attacksimulatorapp` folder.

Before first run install dependencies:
```
yarn install
```

Run app on localhost:
```
yarn start
```

App will be running on localhost port 3001:
```
http://localhost:3001
```

## Running simulations

Simulation can be started using server address with simulation token. Token is
UUID string which can be located in DB table `attacksimulator.simulation`. There 
are several prepared tokens with standalone tests. Simulation can be started 
multiple times.

Campaign simulation can be run with following URL:
```
http://localhost:3001/?simulationToken=180d20a2-34eb-4a67-93c8-e862db1f51c8
```

Once simulation is completed then results and overall score are stored in database.
There are two views which can be used to access raw results and and score in human 
readable format:

Test results are in view `attacksimulator.simulation_results`.

```
SELECT * FROM attacksimulator.simulation_results
ORDER BY outcome_started DESC, id
;
```

Test evaluation and score are in view `scoring.simulation_scoring_result`:

```
SELECT * FROM scoring.simulation_scoring_result
ORDER BY simulation_outcome_id DESC, scoring_definition_id
;
```


