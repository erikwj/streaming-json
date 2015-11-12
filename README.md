# streaming-json
Library for writing json to excel. 

Benchmark directory is WIP, currently go to parser and start SBT over there. 

The maximum number of rows is currently 1048565, because the project has 10 rows in memory and the max for SXSSFSheet == 1048575 ( I guess)

## create json 

`python randjsondata.py 1048565 jsonfiles/maxrows.json`

 
