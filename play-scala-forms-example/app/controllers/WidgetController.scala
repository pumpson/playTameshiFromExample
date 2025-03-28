package controllers

import javax.inject.Inject

import models.Widget
import play.api.data._
import play.api.i18n._
import play.api.mvc._

import scala.collection._

/**
 * MessagesAbstractControllerを使用したクラシックなWidgetController。
 *
 * MessagesAbstractControllerの代わりにI18nSupportトレイトを使用することもできます。
 * これにより、暗黙の変換を使用してリクエストからMessagesインスタンスを作成する
 * 暗黙的なメソッドが提供されます。
 *
 * 詳細はhttps://www.playframework.com/documentation/latest/ScalaForms#Passing-MessagesProvider-to-Form-Helpers
 * を参照してください。
 */
class WidgetController @Inject()(cc: MessagesControllerComponents) extends MessagesAbstractController(cc) {
  import WidgetForm._

  // ウィジェットのリストを保持する可変のArrayBuffer
  private val widgets = mutable.ArrayBuffer(
    Widget("Widget 1", 123),
    Widget("Widget 2", 456),
    Widget("Widget 3", 789)
  )

  // ウィジェット作成のためのURL。テンプレートから直接呼び出すこともできますが、
  // テンプレートをステートレスに保つために、.scalaファイル内で参照することが推奨されます。
  private val postUrl = routes.WidgetController.createWidget

  // ホームページのアクション
  def index = Action {
    Ok(views.html.index())
  }

  // ウィジェットのリストを表示するアクション
  def listWidgets = Action { implicit request: MessagesRequest[AnyContent] =>
    // 未入力のフォームをテンプレートに渡す
    Ok(views.html.listWidgets(widgets.toSeq, form, postUrl))
  }

  // フォームの投稿を処理するアクション
  // memo:`request`が`MessagesRequestAnyContent`であるのはPlay Frameworkの仕様によるもの
  // requestにはスコープ内のどこかで定義されているMessagesRequest型のインスタンスが暗黙的に渡される
  def createWidget = Action { implicit request: MessagesRequest[AnyContent] =>
    // フォームにエラーがある場合の処理
    val errorFunction = { (formWithErrors: Form[Data]) =>
      // フォームにバリデーションエラーがある場合、エラーを強調表示して再表示する
      BadRequest(views.html.listWidgets(widgets.toSeq, formWithErrors, postUrl))
    }

    // フォームが正常に解析された場合の処理
    val successFunction = { (data: Data) =>
      // フォームが正常に解析された場合、ウィジェットを作成してリストに追加する
      // memo:ウィジェットとは単純に名前と価格を持つデータクラス
      val widget = Widget(name = data.name, price = data.price)
      widgets += widget
      // ウィジェットのリストページにリダイレクトし、フラッシュメッセージを表示する
      // memo:	flashingは一時的な性質を持ち、次のリクエストでのみ利用可能です。その後、自動的に削除されます
      Redirect(routes.WidgetController.listWidgets).flashing("info" -> "Widget added!")
    }

    // フォームのバリデーション結果を取得
    val formValidationResult = form.bindFromRequest()
    // バリデーション結果に基づいて適切な関数を呼び出す
    formValidationResult.fold(errorFunction, successFunction)
  }
}