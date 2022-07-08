import json
import csv
import sys
import datetime
import time
from datetime import datetime

# file input
json_file = sys.argv[1]

# Date now for file name
now = datetime.now()
date = now.strftime("%Y%m%d_%H%M%S")

# now we will open a file for writing
data_file = open('%s.csv' % json_file, 'w')

# create the csv writer object
csv_writer = csv.writer(data_file)

# headers to the CSV file
count = 0

# Import JSON file
json_file = open(json_file)

#result_str=json.dumps(json_file.readlines())

for line in json_file:

    # Load into JSON String
    result_str=json.dumps(line)

    # Load result into JSON dictionary
    result_dict = json.loads(result_str)
    result_dict2 = json.loads(result_dict)

    if count == 0:
        # Writing headers of CSV file
        csv_writer.writerow(["poolId","blockHeight","difficulty","networkdifficulty","miner","worker","userAgent","ipAddress","source","created"])
        # Old format, changed to the above to lower clicks when importing via pgAdmin
        #csv_writer.writerow(["poolId","miner","worker","userAgent","ipAddress","source","difficulty","blockHeight","networkDifficulty","created"])
        count += 1

    # Writing data of CSV file
    #csv_writer.writerow(result_dict2.values())
    csv_writer.writerow([
        result_dict2['poolId'],
        result_dict2['blockHeight'],
        result_dict2['difficulty'],
        result_dict2['networkDifficulty'],
        result_dict2['miner'],
        result_dict2['worker'],
        result_dict2['userAgent'],
        result_dict2['ipAddress'],
        result_dict2['source'],
        result_dict2['created']
        ])

data_file.close()
