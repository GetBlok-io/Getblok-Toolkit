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
The output will contain each credit and debit in individual lines, and contain the USD equivalent per CoinGecko.  A description field retains the original ERG amount and the transaction id for reference.

## WARNING
Please use all wallet functions at your own risk. Getblok is NOT responsible for any problems that arise
through use of the CLI wallet services. 
