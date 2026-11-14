<%-- what went wrong with the last thing someone did, if anything --%>
@param String message

@if(message != null)
    <p role="alert" class="alert mt-6">${message}</p>
@endif
