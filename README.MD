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

## Executing Python conversion script:
```shell
python3 fetch_ergo_price.py <csv_containing_ergo_transactions_from_java_toolkit>
```
The output will contain each credit and debit in individual lines, and contain the USD equivalent per CoinGecko.  A description field retains the original ERG amount and the transaction id for reference:

`Date, Amount USD, Transaction, Description`
`29-06-2022,13.124539498162784,3e45fb94b3b43d86fc4d718f2a16f0792a4a2891790fa629f460b23885861d73,Credit ERG: 6.826673667 | Transaction: 3e45fb94b3b43d86fc4d718f2a16f0792a4a2891790fa629f460b23885861d73 | USD Exchange Rate: 1.92253799410488`

## WARNING
Please use all wallet functions at your own risk. Getblok is NOT responsible for any problems that arise
through use of the CLI wallet services. 
