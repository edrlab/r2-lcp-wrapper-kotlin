package org.readium.r2.lcp.Model.Documents

import org.json.JSONObject
import org.readium.r2.lcp.LcpError
import org.readium.r2.lcp.LcpErrorCase
import org.readium.r2.lcp.Model.SubParts.Link
import org.readium.r2.lcp.Model.SubParts.lcp.Encryption
import org.readium.r2.lcp.Model.SubParts.lcp.Rights
import org.readium.r2.lcp.Model.SubParts.lcp.User
import org.readium.r2.lcp.Model.SubParts.lcp.Signature
import java.net.URL
import java.nio.charset.Charset
import java.util.*
import org.joda.time.DateTime
import org.json.JSONArray
import org.readium.r2.lcp.Model.SubParts.parseLinks

/// Document that contains references to the various keys, links to related
/// external resources, rights and restrictions that are applied to the
/// Protected Publication, and user information.
class LicenseDocument {

    var id: String
    /// Date when the license was first issued.
    var issued: String
    /// Date when the license was last updated.
    var updated: String? = null
    /// Unique identifier for the Provider (URI).
    var provider: URL
    // Encryption object.
    var encryption: Encryption
    /// Used to associate the License Document with resources that are not
    /// locally available.
    var links = listOf<Link>()
    var rights: Rights
    /// The user owning the License.
    var user: User
    /// Used to validate the license integrity.
    var signature: Signature
    var json: JSONObject

    // The possible rel of Links.
    enum class Rel(val v:String) {
        hint("hint"),
        publication("publication"),
        status("status")
    }

    constructor(data: ByteArray) {
        val text = data.toString(Charset.defaultCharset())
        try {
            json = JSONObject(text)
        } catch (e: Exception) {
            throw Exception("Lcp parsing error")
        }

        try {
            id = json.getString("id")
            issued = DateTime(json.getString("issued")).toDate().toString()
            provider = URL(json.getString("provider"))
        } catch (e: Exception) {
            throw Exception("Lcp parsing error")
        }
        encryption = Encryption(JSONObject(json.getString("encryption")))
        links = parseLinks(json["links"] as JSONArray)
        rights = Rights(json.getJSONObject("rights"))
        if (json.has("potential_rights")) {
            rights.potentialEnd = DateTime(json.getJSONObject("potential_rights").getString("end")).toDate().toString()
        }
        user = User(json.getJSONObject("user"))
        signature = Signature(json.getJSONObject("signature"))
        if (json.has("updated")) {
            updated = DateTime(json.getString("updated")).toDate().toString()
        }
        if (link("hint") == null){
            throw Exception(LcpError().errorDescription(LcpErrorCase.hintLinkNotFound))
        }
        if (link("publication") == null){
            throw Exception(LcpError().errorDescription(LcpErrorCase.publicationLinkNotFound))
        }
    }

    /// Returns the date of last update if any, or issued date.
    fun dateOfLastUpdate() = if (updated != null) updated!! else issued

    /// Returns the first link containing the given rel.
    ///
    /// - Parameter rel: The rel to look for.
    /// - Returns: The first link containing the rel.
    fun link(rel: String) = links.firstOrNull{
        it.rel.contains(rel)
    }

    fun getHint() = encryption.userKey.hint

}