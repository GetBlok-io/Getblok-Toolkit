package app

import explorer.{ExplorerHandler, Helpers}
import org.ergoplatform.appkit.impl.ErgoTreeContract
import org.ergoplatform.appkit.{Address, InputBox, NetworkType, Parameters, RestApiErgoClient, SecretString}

import scala.collection.JavaConverters.{collectionAsScalaIterableConverter, seqAsJavaListConverter}
import scala.collection.mutable.ArrayBuffer
import scala.util.{Failure, Try}

object BlokWalletApp {
  def run() = {
    val ergoClient = RestApiErgoClient.create("http://213.239.193.208:9053/", NetworkType.MAINNET, "hello", "https://ergo-explorer-cdn.getblok.io/")

    val explorerHandler = new ExplorerHandler(RestApiErgoClient.defaultMainnetExplorerUrl)

    def shellInput(): String = {
      print("BlokWalletApp$" + " ")
      return scala.io.StdIn.readLine()
    }

    def shellPassword(): String = {
      print("BlokWalletApp$" + " ")
      val console = System.console()
      val charArr = console.readPassword()
      String.copyValueOf(charArr)
    }

    def shellPrint(str: String) = {
      print("BlokWalletApp$" + ": " + str + "\n")
    }

    shellPrint("Enter an address to send to: ")
    val addressStr = shellInput()

    val address = Address.create(addressStr)

    shellPrint("Enter your seed phrase:")
    val seedPhrase = SecretString.create(shellPassword())
    val prover = ergoClient.execute {
      ctx =>
        val builder = ctx.newProverBuilder().withMnemonic(seedPhrase, SecretString.empty())
        for (i <- 0 to 100) yield builder.withEip3Secret(i)
        builder.build()
    }
    shellPrint("Enter \"box\" to pay by boxid, enter \"amount\" to pay with specific amount," +
      " or enter \"consolidate\" to consolidate boxes")
    val payType = shellInput()
    payType match {
      case "box" =>
        while (true) {
          shellPrint("Enter a box id to spend:")
          val boxId = shellInput()
          shellPrint("Now sending transaction to spend box")
          val txId = makeTransaction(boxId)
          shellPrint(s"Transaction sent with id: ${txId}")
        }
      case "amount" =>
        shellPrint("Enter the address you're sending from: ")
        val addressFrom = Address.create(shellInput())

        shellPrint("Enter an amount to send, in ERG: ")
        val amountToSend = (shellInput().toDouble * Parameters.OneErg).toLong
        shellPrint("Max number of boxes to query to (try 2000 to start)")
        val toQuery = (shellInput().toInt)
        val boxes = collectDistinctBoxes(addressFrom, toQuery)
        val txId = makeTransactionAmnt(addressFrom, amountToSend, boxes)
        shellPrint(s"Transaction sent with id: ${txId}")
        shellPrint("Now exiting...")
        sys.exit(0)
      case "consolidate" =>
        shellPrint("Enter the address you're consolidating")
        val addressFrom = Address.create(shellInput())
        shellPrint("Max number of boxes to query to (try 2000 to start)")
        val toQuery = (shellInput().toInt)
        shellPrint("Now collecting boxes")
        val boxes = collectDistinctBoxes(addressFrom, toQuery)
        consolidateTx(addressFrom, sliceBoxes(boxes))
    }

    def collectDistinctBoxes(address: Address, toQuery: Int) = {
      val boxBuffer = ArrayBuffer.empty[InputBox]
      var boxesAdded = 0L
      var offset = 0L
      do {
        ergoClient.execute {
          ctx =>
            shellPrint("Collecting next 500 box page from blockchain")
            val optNextBoxes = explorerHandler.boxesByAddress(address, offset.toInt, 500).map(_.items
              .filter(i => i.spendingTxId.isEmpty)
              .map(i => i.id.toString))
            if (optNextBoxes.isDefined) {
              val nextBoxes = ctx.getBoxesById(optNextBoxes.get: _*)
              shellPrint("Page returned")
              boxesAdded = nextBoxes.length
              shellPrint(s"Total of ${boxesAdded} boxes added")
              boxBuffer ++= nextBoxes
              offset = offset + 500
              shellPrint(s"New offset: ${offset}")
            } else {
              throw new Exception("No boxes found")
            }
        }
      } while (offset < toQuery)
      shellPrint("Finished collecting box pages, now creating unique mapping")
      val boxMap = boxBuffer.map(b => b.getId.toString -> b).toMap
      val availableBoxes = boxMap.values.toSeq
      shellPrint(s"Total of ${availableBoxes.length} distinct available boxes")
      availableBoxes
    }

    def sliceBoxes(boxSeq: Seq[InputBox]) = {
      shellPrint(s"Now slicing ${boxSeq.length} boxes")
      val sliced = boxSeq.sliding(100, 100).toSeq
      shellPrint("Boxes succesfully sliced")
      sliced
    }

    def consolidateTx(address: Address, boxSlices: Seq[Seq[InputBox]]) = {
      shellPrint(s"Making consolidation txs with ${boxSlices.length} slices" +
        s" of length 100")
      var counter = 0
      for (slice <- boxSlices) {
        val totalInputs = slice.map(_.getValue.toLong).sum
        val tokensExist = slice.exists(_.getTokens.size() > 0)
        shellPrint(s"Now attempting tx $counter")
        Try {
          ergoClient.execute {
            ctx =>
              val feeToUse = if (tokensExist) Parameters.MinFee * 2 else Parameters.MinFee * 3
              val outBox = ctx.newTxBuilder().outBoxBuilder()
                .value(totalInputs - Parameters.MinFee * 10)
                .contract(address.toErgoContract)
                .build()

              val uTx = ctx.newTxBuilder()
                .boxesToSpend(slice.asJava)
                .outputs(outBox)
                .fee(feeToUse)
                .sendChangeTo(address.getErgoAddress)
                .build()

              val sTx = prover.sign(uTx)
              val txId = ctx.sendTransaction(sTx)

              println(s"Signed tx with id ${txId} was sent!")

          }
        }.recoverWith {
          case exception: Exception =>
            shellPrint(s"There was an exception thrown for a box slice.")
            exception.printStackTrace()
            shellPrint("Ignoring exception to attempt other txs")
            Failure(exception)
        }
        counter = counter + 1
      }
    }

    def makeTransaction(boxId: String) = {
      ergoClient.execute {
        ctx =>
          val inputs = ctx.getBoxesById(boxId)
          val outbox = ctx.newTxBuilder().outBoxBuilder().value(inputs(0).getValue - Parameters.MinFee).contract(address.toErgoContract).build()
          val unsignedTx = ctx.newTxBuilder()
            .boxesToSpend(inputs.toSeq.asJava)
            .outputs(outbox)
            .fee(Parameters.MinFee)
            .sendChangeTo(prover.getAddress.getErgoAddress)
            .build()

          val signedTx = prover.sign(unsignedTx)
          ctx.sendTransaction(signedTx)
      }

    }

    def makeTransactionAmnt(fromAddress: Address, amount: Long, inputBoxes: Seq[InputBox]) = {
      ergoClient.execute {
        ctx =>
          shellPrint(s"Total amount of ERG in collected boxes: ${(BigDecimal(inputBoxes.map(_.getValue.toLong).sum) / Parameters.OneErg).toDouble} ERG")
          shellPrint(s"Amount needed in transaction: ${(BigDecimal(amount) / Parameters.OneErg).doubleValue()} ERG")
          var inputsSorted = inputBoxes.toSeq.sortBy(_.getValue.toLong).reverse
          var currentSum = 0L
          var boxesToSpend = Seq.empty[InputBox]
          while (currentSum < amount + Parameters.MinFee) {
            boxesToSpend = boxesToSpend ++ Seq(inputsSorted.head)
            currentSum = currentSum + inputsSorted.head.getValue.toLong
            inputsSorted = inputsSorted.slice(1, inputsSorted.length)

          }
          val outbox = ctx.newTxBuilder().outBoxBuilder().value(amount).contract(address.toErgoContract).build()
          val unsignedTx = ctx.newTxBuilder()
            .boxesToSpend(boxesToSpend.asJava)
            .outputs(outbox)
            .fee(Parameters.MinFee)
            .sendChangeTo(fromAddress.getErgoAddress)
            .build()

          val signedTx = prover.sign(unsignedTx)
          ctx.sendTransaction(signedTx)
      }
    }


    case class BlockDate(height: Long, blockHash: String) {
      override def toString: String = {
        s"BD(Height: $height, Id: $blockHash)"
      }
    }
  }
}

