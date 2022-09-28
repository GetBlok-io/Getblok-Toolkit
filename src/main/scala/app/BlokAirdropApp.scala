package app

import com.github.tototoshi.csv.CSVReader
import explorer.ExplorerHandler
import org.ergoplatform.appkit._

import scala.collection.JavaConverters.seqAsJavaListConverter
import scala.collection.mutable.ArrayBuffer
import scala.util.{Failure, Try}

object BlokAirdropApp {

  def shellPrint(str: String) = {
    print("BlokWalletApp$" + ": " + str + "\n")
  }

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



    shellPrint("Enter your seed phrase:")
    val seedPhrase = SecretString.create(shellPassword())
    val prover = ergoClient.execute {
      ctx =>
        val builder = ctx.newProverBuilder().withMnemonic(seedPhrase, SecretString.empty())
        for (i <- 0 to 100) yield builder.withEip3Secret(i)
        builder.build()
    }
    shellPrint("Enter your address:")
    val addressStr = shellInput()

    val address = Address.create(addressStr)

    shellPrint("Enter the file path to the CSV you wish to airdrop:")
    val filePath = shellInput()
    val reader = CSVReader.open(filePath)

    shellPrint("Now reading airdrops")
    val airdrops = readAirDrop(reader)

    shellPrint(s"Found a total of ${airdrops.length} airdrops")

    shellPrint("Enter the currency you wish to airdrop")
    shellPrint("Options are: \"ERG\", \"COMET\", \"NETA\", \"ERGOPAD\"")
    val currType = shellInput()
    currType match {
      case "ERG" =>
        sendERGTx(address, airdrops, ergoClient, prover)
      case "NETA" =>
        sendTokenTx(address, airdrops, ergoClient, prover, ErgoId.create("472c3d4ecaa08fb7392ff041ee2e6af75f4a558810a74b28600549d5392810e8"),
          1000000
        )
      case "ERGOPAD" =>
        sendTokenTx(address, airdrops, ergoClient, prover, ErgoId.create("d71693c49a84fbbecd4908c94813b46514b18b67a99952dc1e6e4791556de413"),
          100
        )
      case "GBGT" =>
        sendTokenTx(address, airdrops, ergoClient, prover, ErgoId.create("60a3b2e917fe6772d65c5d253eb6e4936f1a2174d62b3569ad193a2bf6989298"),
          1000000000
        )
      case "PAIDEIA" =>
        sendTokenTx(address, airdrops, ergoClient, prover, ErgoId.create("1fd6e032e8476c4aa54c18c1a308dce83940e8f4a28f576440513ed7326ad489"),
          10000
        )
      case "COMET" =>
        sendTokenTx(address, airdrops, ergoClient, prover, ErgoId.create("0cd8c9f416e5b1ca9f986a7f10a84191dfb85941619e49e53c0dc30ebf83324b"),
          1
        )
      case _ =>
        shellPrint("Invalid currency picked!")
    }
  }

  case class AirDrop(address: Address, double: Double)

  def makeERGBox(txB: UnsignedTransactionBuilder, airDrop: AirDrop) = {
    txB.outBoxBuilder()
      .value((airDrop.double * Parameters.OneErg).toLong)
      .contract(airDrop.address.toErgoContract)
      .build()
  }

  def makeTokenBox(txB: UnsignedTransactionBuilder, airDrop: AirDrop, tokenId: ErgoId, decimals: Long) = {
    txB.outBoxBuilder()
      .value(Parameters.MinFee)
      .contract(airDrop.address.toErgoContract)
      .tokens(new ErgoToken(tokenId, (airDrop.double * decimals).toLong))
      .build()
  }

  def readAirDrop(reader: CSVReader) = {
    val lines = reader.iterator.toSeq

    for(l <- lines) yield {
      shellPrint(s"Address: ${l.head}")
      shellPrint(s"Amount: ${l.last.toDouble}")
      AirDrop(Address.create(l.head), l.last.toDouble)
    }
  }

  val txFee = Parameters.MinFee * 5

  def sendERGTx(address: Address, airdrops: Seq[AirDrop], ergoClient: ErgoClient, prover: ErgoProver) = {
    val totalAmnt = airdrops.map( a => (a.double * Parameters.OneErg).toLong ).sum

    shellPrint(s"Sending a total of ${totalAmnt} nanoErgs")

    ergoClient.execute{
      ctx =>
        val boxes = ctx.getCoveringBoxesFor(address, txFee + totalAmnt, Seq[ErgoToken]().asJava).getBoxes
        val outBoxes = airdrops.map(a => makeERGBox(ctx.newTxBuilder(), a))

        val uTx = ctx.newTxBuilder()
          .boxesToSpend(boxes)
          .outputs(outBoxes: _*)
          .fee(txFee)
          .sendChangeTo(address.getErgoAddress)
          .build()

        shellPrint("Now signing transaction.")

        val signed = prover.sign(uTx)

        shellPrint(s"Now sending transaction with id ${signed.getId}")

        ctx.sendTransaction(signed)

        shellPrint("Transaction sent successfully!")
    }
  }

  def sendTokenTx(address: Address, airdrops: Seq[AirDrop], ergoClient: ErgoClient, prover: ErgoProver, tokenId: ErgoId, decimals: Long) = {
    val totalAmnt = airdrops.map( a => (a.double * decimals).toLong ).sum
    val totalErg = airdrops.length * Parameters.MinFee
    shellPrint(s"Sending a total of ${totalAmnt} tokens")
    shellPrint(s"Sending a total of ${totalErg} nanoErgs")

    ergoClient.execute{
      ctx =>
        val boxes = ctx.getCoveringBoxesFor(address, txFee + totalErg, Seq[ErgoToken](new ErgoToken(tokenId, totalAmnt)).asJava).getBoxes
        val outBoxes = airdrops.map(a => makeTokenBox(ctx.newTxBuilder(), a, tokenId, decimals))

        val uTx = ctx.newTxBuilder()
          .boxesToSpend(boxes)
          .outputs(outBoxes: _*)
          .fee(txFee)
          .sendChangeTo(address.getErgoAddress)
          .build()

        shellPrint("Now signing transaction.")

        val signed = prover.sign(uTx)

        shellPrint(s"Now sending transaction with id ${signed.getId}")

        ctx.sendTransaction(signed)

        shellPrint("Transaction sent successfully!")
    }
  }
}

