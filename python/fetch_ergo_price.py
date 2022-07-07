import json
import csv
import sys
import datetime
import time
from datetime import datetime
from pycoingecko import CoinGeckoAPI

# CoingGecko API
cg = CoinGeckoAPI()

# Params at command line
# 1 = csv file to process
file_csv = sys.argv[1]

# Format of inbound timestamp
# 2022-06-28
fmt = '%Y-%m-%d'

# Setup multiplication for float and decimals
def multiply(x,y):
    x = float(x) * float(y)
    return round(x, 16);

# Date now for file name
now = datetime.now()
date = now.strftime("%Y%m%d_%H%M%S")

# Write header
file = open('qbo_import-%s.csv' % date , 'a')
file_output =  "Date, Amount USD, Transaction, Description"
file.write(file_output + "\n")
#file.close

# Defaults
credit_usd = float(0.0)
debit_usd = float(0.0)

## Fetch Ergo for particular date
## from CoinGecko API
with open(file_csv, newline='') as f:
    r = csv.DictReader(f)

    # Cycle through each row from blockchain ledger
    for row in r:
        row_date = datetime.strptime(row['Date'],fmt)
        row_newdate = row_date.strftime('%d-%m-%Y')

        # Fetch CoinGecko data for the date
        result = cg.get_coin_history_by_id(id='ergo', date=row_newdate)
        result_str = json.dumps(result)

        # Load result into JSON dictionary
        result_dict = json.loads(result_str)

        # Extract USD price
        result_json_current_price = result_dict['market_data']['current_price']['usd']

        # Calculate USD price
        credit_usd = multiply(row['Credit'], result_json_current_price)
        debit_usd = -abs(multiply(row['Debit'], result_json_current_price))

        # Populate description field to add to QBO ledger description
        description_credit = "Credit ERG: " + str(row['Credit']) + " | Transaction: " + str(row['Transaction']) + " | USD Exchange Rate: " + str(result_json_current_price)
        
        description_debit = "Debit ERG: " + str(row['Debit']) + " | Transaction: " + str(row['Transaction']) + " | USD Exchange Rate: " + str(result_json_current_price)

        # Output CREDIT
        if credit_usd > float(0.0):
            print(row_newdate, ',', credit_usd, ',', row['Transaction'], ',', description_credit)
            file_output =  row_newdate + "," + str(credit_usd) + "," + str(row['Transaction']) + "," + str(description_credit)
            file.write(file_output + "\n")

        # Output DEBIT
        if debit_usd < float(0.0):
            print(row_newdate, ',', debit_usd, ',', row['Transaction'], ',', description_debit )
            file_output =  row_newdate + "," + str(debit_usd) + "," + str(row['Transaction']) + "," + str(description_debit)
            file.write(file_output + "\n")
        
        file.flush()

        time.sleep(2)

file.close
