package filters

import play.api.http.DefaultHttpFilters
import play.filters.cors.CORSFilter

import javax.inject._

@Singleton
class Filters @Inject()(corsFilter: CORSFilter, authFilter: AuthFilter)
  extends DefaultHttpFilters(corsFilter, authFilter)
