package explorer

import explorer.Models._
import okhttp3.OkHttpClient
import org.ergoplatform.appkit.{Address, ErgoId, NetworkType, RestApiErgoClient}
import org.ergoplatform.explorer.client.model._
import org.ergoplatform.explorer.client.{DefaultApi, ExplorerApiClient}
import retrofit2.Response
import sigmastate.Values.ErgoTree

import java.util.concurrent.TimeUnit
import scala.collection.JavaConverters.collectionAsScalaIterableConverter

class ExplorerHandler(url: String) {


  private val httpClientBuilder = new OkHttpClient()
    .newBuilder()
    .callTimeout(60, TimeUnit.SECONDS)
    .readTimeout(60, TimeUnit.SECONDS)
    .writeTimeout(60, TimeUnit.SECONDS)
    .connectTimeout(60, TimeUnit.SECONDS)


  private val apiClient = new ExplorerApiClient(url)
  apiClient.configureFromOkClientBuilder(httpClientBuilder)
  private val apiService: CustomAPI = apiClient.createService(classOf[CustomAPI])

  private def asOption[T](resp: Response[T]): Option[T] = {
    if (resp.isSuccessful)
      Some(resp.body())
    else {
      None
    }
  }

  private def itemSeq[T](opt: Option[Items[T]]) = {
    if (opt.isDefined)
      Some(opt.get.getItems.asScala.toSeq)
    else
      None
  }

  private def outputSeq(opt: Option[ItemsA]) = {
    if (opt.isDefined)
      Some(ExplorerOutputItems(opt.get.getItems.asScala.toSeq, opt.get.getTotal.longValue()))
    else
      None
  }

  /**
   * Get data about a transaction using it's id
   * @param id ErgoId of transaction
   */
  def getTransaction(id: ErgoId): Option[TransactionData] = {
    TransactionData.fromOption(asOption[TransactionInfo](apiService.getApiV1TransactionsP1(id.toString).execute()))
  }

  /**
   * Get transactions that were sent and received by the given address
   * @param addr Address to check
   * @param offset Number of transactions to offset
   * @param limit Max number of transactions in response
   */
  def txsForAddress(addr: Address, offset: Int = 0, limit: Int = 10): Option[Seq[TransactionData]] = {
    val opt = asOption[Items[TransactionInfo]](apiService.getApiV1AddressesP1Transactions(addr.toString, offset, limit).execute())
    TransactionData.fromOptionSeq(itemSeq[TransactionInfo](opt))
  }

  /**
   * Get boxes under addresses that match a given ErgoTree template hash
   * @param hash Hash of template
   * @param offset Number of boxes to offset
   * @param limit Max number of boxes in response
   */
  def boxesByTemplateHash(hash: String, offset: Int = 0, limit: Int = 10) = {
    OutputItems.fromOption(
      outputSeq(asOption[ItemsA](apiService.getApiV1BoxesByergotreetemplatehashP1(hash, offset, limit).execute()))
    )
  }
  /**
   * Get boxes under addresses that match a given ErgoTree
   * @param ergoTree ErgoTree to find boxes for
   * @param offset Number of boxes to offset
   * @param limit Max number of boxes in response
   */
  def boxesByErgoTree(ergoTree: ErgoTree, offset: Int = 0, limit: Int = 10) = {
    OutputItems.fromOption(
      outputSeq(asOption[ItemsA](apiService.getApiV1BoxesByergotreeP1(ergoTree.bytesHex, offset, limit).execute()))
    )
  }
  /**
   * Get boxes under addresses that match a given ErgoTree hexadecimal string
   * @param ergoTreeHex ErgoTree hex string to find boxes for
   * @param offset Number of boxes to offset
   * @param limit Max number of boxes in response
   */
  def boxesByErgoTreeHex(ergoTreeHex: String, offset: Int = 0, limit: Int = 10) = {
    OutputItems.fromOption(
      outputSeq(asOption[ItemsA](apiService.getApiV1BoxesByergotreeP1(ergoTreeHex, offset, limit).execute()))
    )
  }
  /**
   * Get boxes under a certain address
   * @param address Address to find boxes for
   * @param offset Number of boxes to offset
   * @param limit Max number of boxes in response
   */
  def boxesByAddress(address: Address, offset: Int = 0, limit: Int = 10) = {
    OutputItems.fromOption(
      outputSeq(asOption[ItemsA](apiService.getApiV1BoxesUnspentByaddressP1(address.toString, offset, limit, "asc").execute()))
    )
  }
  /**
   * Get boxes that contain a token with the given id
   * @param tokenId ErgoId of token that the requested boxes must contain
   * @param offset Number of boxes to offset
   * @param limit Max number of boxes in response
   */
  def boxesByTokenId(tokenId: ErgoId, offset: Int = 0, limit: Int = 10) = {
    OutputItems.fromOption(
      outputSeq(asOption[ItemsA](apiService.getApiV1BoxesUnspentBytokenidP1(tokenId.toString, offset, limit).execute()))
    )
  }

  /**
   * Get a box with the given id
   * @param id Id of box to search for
   */
  def boxesById(id: ErgoId): Option[Output] = {
      Output.fromOption(asOption[OutputInfo](apiService.getApiV1BoxesP1(id.toString).execute()))
  }

  /**
   * Get the total balance of confirmed and unconfirmed ERG and Tokens under the given address
   * @param address Address to find total balance for
   */
  def getTotalBalance(address: Address): Option[FullBalance] = {
    FullBalance.fromOption(asOption[TotalBalance](apiService.getApiV1AddressesP1BalanceTotal(address.toString).execute()))
  }

  /**
   * Get balance for address that only accounts for boxes with a minimum confirmation number
   * @param address Address to get balance for
   * @param minConfirmations Minimum number of confirmations for a box under the address to be included in the balance
   */
  def getConfirmedBalance(address: Address, minConfirmations: Int = 20): Option[AddressBalance] = {
    AddressBalance.fromOption(asOption[Balance](apiService.getApiV1AddressesP1BalanceConfirmed(address.toString, minConfirmations).execute()))
  }

  def getBlockById(blockId: ErgoId): Option[BlockContainer] = {
    BlockContainer.fromOption(asOption[BlockSummary](apiService.getApiV1BlocksP1(blockId.toString).execute()))
  }





}