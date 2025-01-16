// =====================================================
// UpScan upload
// =====================================================
$("#uploadForm").submit(function(e){
    const fileLength = $("#file-input")[0].files.length;
    if (fileLength === 0) {
//        var errorRequestId = $("#x-amz-meta-request-id").val();
//        var errorUrl = $("#upScanErrorRedirectUrl").val() + "?errorCode=InvalidArgument&errorMessage=FileNotSelected&errorRequestId=" + errorRequestId;
//        window.location = errorUrl;
    } else {
        e.preventDefault();
        function disableFileUpload(){
            $("#file-input").attr('disabled', 'disabled')
        }

        function addUploadSpinner() {
            $("#processing").empty();
            $("#processing").append('<div id="spinner-svg"><svg class="ccms-loader" height="100" width="100"><circle cx="50" cy="50" r="40"  fill="none"/></svg></div><div id="spinner-text"><p style="float:left">' + $("#processingMessage").val() + '</p></div>');
            $(".govuk-form-group--error").removeClass("govuk-form-group--error");
            $("#file-input-error").remove();
            $("#error-summary").remove();
            $("#submit").remove();
        }

        addUploadSpinner();
        setTimeout(function() {
            this.submit();
            disableFileUpload();
        }.bind(this), 0);
    }
});

$(document).ready(function ()
{
    var hasError = (window.location.href.indexOf("errorCode") > -1);
    var preFixError = hasError ? "Error: " : "";
    var appendError = preFixError + $("title").html();
    $("title").html(appendError);
});