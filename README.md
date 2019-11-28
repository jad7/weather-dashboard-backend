# weather-dashboard-backend
#### REST API
####`/sensors/v1/day` - detail points for the last day
##### Params: 
 - sensors  - comma-separated list of sensor name
##### Example:
```
{
    "outside": [
        {
            "value": 0.78,
            "time": "2019-11-28T01:37:41.678Z"
        },
        {
            "value": -0.83,
            "time": "2019-11-28T01:37:43.673Z"
        }
         ...
    ],
    "inside": [
        {
            "value": 23.21,
            "time": "2019-11-28T01:37:39.675Z"
        },
        {
            "value": 23.78,
            "time": "2019-11-28T01:37:41.677Z"
        }
        ...
    ]
}
```
`/sensors/v1/period` - information including history files 
##### Params: 
 - sensors  - comma-separated list of sensor name
 - fromEpochMillis - information starting from epoch time in millis
 - toEpochMillis - information ending at epoch time in millis
 
##### Example:
The same like in `/sensors/v1/day` resource. 

#### Web-Socket
`/sensors/v1/realtime` updates ~ every 10sec for each sensor state:
```
{
    "data": {
        "sensorName": "outside"
        "value": 23.12
        "time": "2019-11-28T01:37:43.673Z"
    }
}
```

#### Possible sensor names:
For test:
 - inside
 - outside
For prod:
 - outside
 - outside2
 - centerRoom
 - nearWindow
 - humidity

