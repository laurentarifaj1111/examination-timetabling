# examination-timetabling

## Setup

Make sure to have java 17 and gradle 8.1.1 installed. Navigate to the root directory and run

`./gradlew run`

Run the code using

`./gradlew run --args="<instance> <algorithm>"`

For example `./gradlew run --args="D1-1-16 SA"` will run instance `D1-1-16` using Simulated Annealing

Algorithm options:

- VNS - Harmony Search
- SA - Simulated Annealing
- HYBRID - Hybrid algorithm (mix of SA and VNS)
