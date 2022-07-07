# Getblok Toolkit

This CLI application provides the following services:

- Accounting
  - Export transactions to CSV
- Wallet
  - Pay by boxid
  - Pay an amount to an address
  - Large scale consolidation of UTXOs

To run the application, download a release and run the following command:

```shell
java -jar ./GetblokToolkit_1.0.0
```

Make sure to replace the release version at the end of the command with whatever release version you downloaded.

## fetch_ergo_price.py: Convert ERG csv export of wallet history of transactions to QBO-ready CSV Import

To convert the output of the java toolkit to a csv format that can be imported into Quickbooks or any accounting software containing USD equivalent of the transaction (credit or debit) at the time of execution, you can use the python script located under `python/` directory.

To run it, you must install the coingecko python module located at https://github.com/man-c/pycoingecko.

### Installation of PyCoingecko module
PyPI
```bash
pip install pycoingecko
```
or from source
```bash
git clone https://github.com/man-c/pycoingecko.git
cd pycoingecko
python3 setup.py install
```

### Executing Python conversion script:
```shell
python3 fetch_ergo_price.py <csv_containing_ergo_transactions_from_java_toolkit>
```
The output will contain each credit and debit in individual lines, and contain the USD equivalent per CoinGecko.  A description field retains the original ERG amount and the transaction id for reference:

`Date, Amount USD, Transaction, Description`  
`29-06-2022,13.124539498162784,3e45fb94b3b43d86fc4d718f2a16f0792a4a2891790fa629f460b23885861d73,Credit ERG: 6.826673667 | Transaction: 3e45fb94b3b43d86fc4d718f2a16f0792a4a2891790fa629f460b23885861d73 | USD Exchange Rate: 1.92253799410488`  

## json2csv.py: Normalize MiningCore recovered-shares.txt to import via pgAdmin
MiningCore will save shares to the file system, whenever those shares are submitted to a stratum but unable to be inserted to the database.  It is important to import those saved shares back to the `shares` table of the PostgreSQL MC database as soon as possible, but often that is not possible.  

This script will take input any MiningCore `recovered-shares.txt` file and convert to an import-ready CSV via pgAdmin.  NOTE: you still must remove the comments in the `recovered-shares.txt` file, and configure your pgAdmin import job correctly.  We suggest first importing to a temp table, then to the `shares` table.

### Executing Python conversion script:
```shell
python3 json2csv.py recovered_shares.txt
```

### Importing into pgAdmin
This assumes you have experience in using pgAdmin.  BE CAREFUL!!!

<img src="https://user-images.githubusercontent.com/6125290/177885507-fc5bee1e-7bbb-4291-a60a-3dc2a927fa0f.png" width=50% height=50%>

<img src="https://user-images.githubusercontent.com/6125290/177885567-b47a2529-6e9d-45f4-b5b6-37d5dd751509.png width=50% height=50%>


## WARNING
Please use all wallet functions at your own risk. Getblok is NOT responsible for any problems that arise
through use of the CLI wallet services. 
