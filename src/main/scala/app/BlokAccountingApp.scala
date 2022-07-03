package app

import com.github.tototoshi.csv.CSVWriter
import explorer.Models.TransactionData
import explorer.{ExplorerHandler, Helpers}
import org.ergoplatform.appkit.{Address, NetworkType, Parameters, RestApiErgoClient}
import org.ergoplatform.explorer.client.ExplorerApiClient

import java.io.File
import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDateTime, ZoneId}
import java.util.Date
import javax.print.attribute.DateTimeSyntax
import scala.util.{Failure, Try}

object BlokAccountingApp{
  def run() = {

    def shellInput(): String = {
      print("BlokAccountingApp$" + " ")
      return scala.io.StdIn.readLine()
    }

    val ergoClient = RestApiErgoClient.create("http://213.239.193.208:9053/", NetworkType.MAINNET, "hello", "https://ergo-explorer-cdn.getblok.io/")

    val explorerHandler = new ExplorerHandler(RestApiErgoClient.defaultMainnetExplorerUrl)
    val csvFile = new File("out.csv")
    val writer = CSVWriter.open(csvFile)
    writer.writeRow(Seq("Date", "Credit", "Debit", "Difference", "Transaction"))
    println("Enter an address to search: ")
    val addressStr = shellInput()
    val addressToUse = Address.create(addressStr)
    println("Now building csv for address. If it fails on first run, try again!")
    checkTransactions(addressToUse)

    def makeErg(long: Long) = {
      (BigDecimal(long) / Parameters.OneErg).toDouble.toString
    }



    def formatTx(tx: TransactionData, address: Address) = {
      val date = Instant.ofEpochMilli(tx.time).atZone(ZoneId.systemDefault())
      println(s"tx date: ${date.format(DateTimeFormatter.ISO_LOCAL_DATE)}")
      val credit = tx.outputs.filter(_.address.toString == address.toString).map(_.value).sum
      val debit = tx.inputs.filter(_.address.toString == address.toString).map(_.value).sum
      val difference = credit - debit
      val txId = tx.id.toString
      writer.writeRow(Seq(date.format(DateTimeFormatter.ISO_LOCAL_DATE), makeErg(credit), makeErg(debit), makeErg(difference), txId.toString))

    }

    def checkTransactions(address: Address) = {
      var initTxs = explorerHandler.txsForAddress(address, 0, 10)
      var txMap = Map.empty[String, Seq[String]]
      var offset = 0
      var uniqueHits = 1L
      println("Initiating tx checking")
      while (initTxs.isDefined && uniqueHits != 0) {
        Try {
          println("Found tx")
          uniqueHits = 0
          for (tx <- initTxs.get) {
            if (!txMap.contains(tx.id.toString)) {
              uniqueHits = uniqueHits + 1
              formatTx(tx, address)
            }
          }
          offset = offset + 10
          println(s"Total unique hits: ${uniqueHits}")
          initTxs = explorerHandler.txsForAddress(address, offset, 10)
        }.recoverWith {
          case exception: Exception =>
            println("There was a fatal exception")
            Failure(exception)
        }
      }
    }

    def getBalances(address: Address) = {
      var initBoxes = explorerHandler.boxesByAddress(address, limit = 100)
      val totalAmount = initBoxes.get.total
      var offset = 0
      var totalSumUnspent = BigInt(0L)
      var totalSumSpent = BigInt(0L)
      println("Total boxes to evaluate: " + totalAmount)
      while (initBoxes.isDefined && offset <= totalAmount) {
        Try {
          for (b <- initBoxes.get.items) {
            if (b.spendingTxId.isDefined) {
              totalSumSpent = totalSumSpent + b.value

            } else {
              totalSumUnspent = totalSumUnspent + b.value
            }
          }
          println(s"Offset: $offset")
          offset = offset + 100
          initBoxes = explorerHandler.boxesByAddress(address, offset, limit = 100)
        }.recoverWith {
          case ex: Exception =>
            println("Got a timeout from explorer, here is the amount recovered:")
            println(s"Total amount unspent: ${Helpers.nanoErgToErg(totalSumUnspent)}")
            println(s"Total amount spent: ${Helpers.nanoErgToErg(totalSumSpent)}")
            Failure(ex)
        }
      }
      println(s"Total amount unspent: ${Helpers.nanoErgToErg(totalSumUnspent)}")
      println(s"Total amount spent: ${Helpers.nanoErgToErg(totalSumSpent)}")
      println(s"Total value sum: ${Helpers.nanoErgToErg(totalSumSpent) + Helpers.nanoErgToErg(totalSumUnspent)}")
    }

    def getFeesAccrued(address: Address) = {
      var initBoxes = explorerHandler.boxesByAddress(address, limit = 500)
      var offset = 0
      val totalAmount = initBoxes.get.total
      var totalSumUnspent = BigInt(0L)
      var totalSumSpent = BigInt(0L)
      var currentHeight = 0L
      var boxMap = Map.empty[String, Boolean]
      ergoClient.execute {
        ctx =>
          currentHeight = ctx.getHeight
      }
      var currentBlockDate = BlockDate(currentHeight, "")
      println("Total boxes to evaluate: " + totalAmount)
      while (initBoxes.isDefined && boxMap.size < totalAmount) {
        Try {
          for (b <- initBoxes.get.items) {
            if (!boxMap.contains(b.id.toString)) {
              if (b.spendingTxId.isDefined) {
                totalSumSpent = totalSumSpent + b.value

              } else {
                totalSumUnspent = totalSumUnspent + b.value
              }

              if (b.heightCreated < currentBlockDate.height) {
                currentBlockDate = BlockDate(b.heightCreated, b.blockId.toString)
              }
              boxMap = boxMap ++ List(b.id.toString -> true)
            }
          }
          println(s"Offset: $offset, BoxMap: ${boxMap.size}")
          offset = offset + 50
          if (offset > totalAmount)
            offset = 0
          initBoxes = explorerHandler.boxesByAddress(address, offset, limit = 100)
        }.recoverWith {
          case ex: Exception =>
            println("Got a timeout from explorer, here is the amount recovered:")
            println(s"Total amount unspent: ${Helpers.nanoErgToErg(totalSumUnspent)}")
            println(s"Total amount spent: ${Helpers.nanoErgToErg(totalSumSpent)}")
            Failure(ex)
        }
      }
      println(s"Total amount unspent: ${Helpers.nanoErgToErg(totalSumUnspent)}")
      println(s"Total amount spent: ${Helpers.nanoErgToErg(totalSumSpent)}")
      println(s"Total value sum: ${Helpers.nanoErgToErg(totalSumSpent) + Helpers.nanoErgToErg(totalSumUnspent)}")
      println(s"Total value for all wallets: ${3 * (Helpers.nanoErgToErg(totalSumSpent) + Helpers.nanoErgToErg(totalSumUnspent))}")
      println(s"Oldest Block Date: ${currentBlockDate}")
      println(s"Total boxes added to box map: ${boxMap.size}")
    }

    case class BlockDate(height: Long, blockHash: String) {
      override def toString: String = {
        s"BD(Height: $height, Id: $blockHash)"
      }
    }
  }
}

