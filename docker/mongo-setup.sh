#!/bin/bash
echo "Waiting for MongoDB nodes to start..."
sleep 15

mongosh --host mongo1:27017 <<EOF
var config = {
    "_id": "rs0",
    "version": 1,
    "members": [
        {
            "_id": 0,
            "host": "mongo1:27017",
            "priority": 2
        },
        {
            "_id": 1,
            "host": "mongo2:27018",
            "priority": 1
        },
        {
            "_id": 2,
            "host": "mongo3:27019",
            "priority": 1
        }
    ]
};
rs.initiate(config, { force: true });
rs.status();
EOF
