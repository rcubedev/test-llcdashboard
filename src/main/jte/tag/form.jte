@import gg.jte.Content
@import org.limelightcreatives.dashboard.web.Page

<%-- a form that posts: adds the CSRF token --%>
@param Page page
@param String action
@param String classes = null
@param String id = null
@param boolean unsaved = false
@param boolean manual = false
@param Content content

<form method="post" action="${action}" class="${classes}" id="${id}" data-unsaved="${unsaved ? "" : null}" data-manual="${manual ? "" : null}">
    @if(page.csrf() != null)
        <input type="hidden" name="${page.csrf().getParameterName()}" value="${page.csrf().getToken()}">
    @endif
    ${content}
</form>
