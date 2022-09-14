package app

object ToolkitApp extends App{
  def shellPrint(str: String) = {
    print("BlokToolkit$" + ": " + str + "\n")
  }

  def shellInput(): String = {
    print("BlokToolkit$" + " ")
    return scala.io.StdIn.readLine()
  }

  shellPrint("Welcome to the Getblok Toolkit app!")
  shellPrint("Please enter \"accounting\" to make a csv for a wallet.")
  shellPrint("Enter \"wallet\" to go to the console wallet. USE THE WALLET AT YOUR OWN RISK!")
  shellPrint("Enter \"airdrop\" to go to the console airdrop tool. USE THE AIRDROP TOOL AT YOUR OWN RISK!")
  val input = shellInput()

  input match {
    case "accounting" =>
      shellPrint("Now entering accounting app")
      BlokAccountingApp.run()
    case "wallet" =>
      shellPrint("Now entering wallet app")
      BlokWalletApp.run()
    case "airdrop" =>
      shellPrint("Now entering airdrop app")
      BlokAirdropApp.run()
    case _ =>
      shellPrint("That command could not be recognized!")
      shellPrint("Now exiting...")
  }
}
