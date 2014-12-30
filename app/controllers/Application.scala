package controllers

import play.api.mvc._

import scala.concurrent._
import play.api.libs.concurrent.Execution.Implicits._

import io.prismic._

object Application extends Controller with PrismicController {

  import PrismicHelper._

  // -- Resolve links to documents
  def linkResolver(api: Api)(implicit request: RequestHeader) = DocumentLinkResolver(api) {
    // For "Bookmarked" documents that use a special page
    case (Fragment.DocumentLink(_, _, _, _, _), Some("about")) => routes.Application.about.absoluteURL()
    case (Fragment.DocumentLink(_, _, _, _, _), Some("booking")) => routes.Application.booking.absoluteURL()

      // Store documents
    case (Fragment.DocumentLink(id, "about", _, slug, false), _) => routes.Application.about.absoluteURL()

    case (Fragment.DocumentLink(id, docType, tags, slug, false), maybeBookmarked) => routes.Application.detail(id, slug).absoluteURL()
    case (link@Fragment.DocumentLink(_, _, _, _, true), _)                        => routes.Application.brokenLink().absoluteURL()

  }

  // -- Page not found
  def PageNotFound(implicit ctx: PrismicHelper.Context) = NotFound(views.html.pageNotFound())

  def brokenLink = PrismicAction { implicit request =>
    Future.successful(PageNotFound)
  }

  // -- Home page
  def indexp(page: Int) = PrismicAction { implicit request =>
    ctx.api.forms("angebot").ref(ctx.ref).pageSize(10).orderings("[my.article.order]").page(page).submit() map { response =>
      Ok(views.html.indexp(response))
    }
  }

  // -- Home page
  def index = PrismicAction { implicit request =>
    for {
      angebot <- ctx.api.forms("angebot").ref(ctx.ref).pageSize(10).orderings("[my.article.order]").submit()
      firma <- ctx.api.forms("everything").ref(ctx.ref).query(Predicate.at("document.type", "copy")).submit()
      services <- ctx.api.forms("everything").ref(ctx.ref).query(Predicate.at("document.type", "service")).orderings("[my.service.order]").submit()
    } yield {
      Ok(views.html.index(firma.results.head, services.results, angebot.results))
    }
  }



  // -- About us
  def about = PrismicAction { implicit request =>
    for {
      ueber <- ctx.api.forms("ueberuns").ref(ctx.ref).pageSize(10).orderings("[my.article.order]").submit()
      firma <- ctx.api.forms("everything").ref(ctx.ref).query(Predicate.at("document.type", "copy")).submit()
    } yield {
      Ok(views.html.about(firma.results.head, ueber.results))
    }
  }

  // -- Booking
  def booking = PrismicAction { implicit request =>
    for {
    termin <- ctx.api.forms("termin").ref(ctx.ref).pageSize(10).orderings("[my.article.order]").submit()
    firma <- ctx.api.forms("everything").ref(ctx.ref).query(Predicate.at("document.type", "copy")).submit()
  } yield {
      Ok(views.html.booking(firma.results.head, termin.results))
    }
  }

  // -- FAQ

  def faq = PrismicAction { implicit request =>
    for {
    faq <- ctx.api.forms("faq").ref(ctx.ref).pageSize(20).orderings("[my.article.order]").submit()
    firma <- ctx.api.forms("everything").ref(ctx.ref).query(Predicate.at("document.type", "copy")).submit()
  } yield {
      Ok(views.html.faq(firma.results.head, faq))
    }
  }

  // -- AGB

  def bedingungen = PrismicAction { implicit request =>
    for {
      maybePage <- getBookmark("agb")
      firma <- ctx.api.forms("everything").ref(ctx.ref).query(Predicate.at("document.type", "copy")).submit()

    } yield {
      maybePage.map(page => Ok(views.html.detail(firma.results.head, page))).getOrElse(PageNotFound)
    }
  }





  // -- Home old page
  def indexold = Action { implicit request =>
      Ok(views.html.indexold())

  }

  // -- Document detail
  def detail(id: String, slug: String) = PrismicAction { implicit request =>
    for {
      maybeDocument <- getDocument(id)
      firma <- ctx.api.forms("everything").ref(ctx.ref).query(Predicate.at("document.type", "copy")).submit()
    } yield {
      checkSlug(maybeDocument, slug) {
        case Left(newSlug)   => MovedPermanently(routes.Application.detail(id, newSlug).url)
        case Right(document) => Ok(views.html.detail(firma.results.head, document))
      }
    }
  }

  // -- Basic Search
  def search(q: Option[String], page: Int) = PrismicAction { implicit request =>
    ctx.api.forms("everything")
      .query(Predicate.fulltext("document", q.getOrElse("")))
      .ref(ctx.ref).pageSize(10).page(page).submit() map { response =>
        Ok(views.html.search(q, response))
      }
  }

  // -- Preview Action
  def preview(token: String) = PrismicAction { implicit req =>
    ctx.api.previewSession(token, ctx.linkResolver, routes.Application.index().url).map { redirectUrl =>
      Redirect(redirectUrl).withCookies(Cookie(Prismic.previewCookie, token, path = "/", maxAge = Some(30 * 60 * 1000), httpOnly = false))
    }
  }

}
