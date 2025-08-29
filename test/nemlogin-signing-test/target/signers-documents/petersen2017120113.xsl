<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:fn="http://www.w3.org/2005/xpath-functions"
                exclude-result-prefixes="fn"
                version="3.0">
    <!-- using "xhtml" omits SignHTML invalid <meta> element in <head> -->
    <xsl:output method="html" indent="yes" omit-xml-declaration="yes"/>
    <xsl:template match="/">
        <html>
            <head>
                <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
                <style>
                    body {
                        font-family: Times-Roman;
                    }
                    h1 {
                        color: red;
                    }
                    .author {
                        overflow:hidden;
                        background-color: lightgrey
                    }
                    .author-row {
                        font-family: Helvetica;
                        font-size: 12px;
                    }
                    .author-row th {
                        line-height: 32px;
                        font-size: 16px;
                    }
                    .toc {
                        overflow:hidden;
                        margin-top: 20px;
                        background-color: white;
                        border: 1px solid lightgrey;
                    }
                    .toc-row {
                        font-family: Helvetica;
                        font-size: 12px;
                    }
                    .toc-row th {
                        line-height: 32px;
                        font-size: 16px;
                    }
                </style>
            </head>
            <body>
                <h1>
                    <xsl:value-of select="//fn:map[@key='text']/fn:string[@key='title']"/>
                </h1>

                <div style="float: right">
                  <div class="author">
                    <table border="0" cellspacing="0" cellpadding="5" width="300" align="left">
                        <tr class="author-row">
                            <th colspan="2" align="center" valign="middle">Forfatterbiografi</th>
                        </tr>
                        <tr class="author-row">
                            <td align="right"><i>Forfatter</i></td>
                            <td>
                                <xsl:value-of select="//fn:string[@key='firstname']"/>&#160;<xsl:value-of select="//fn:string[@key='lastname']"/>
                            </td>
                        </tr>
                        <tr class="author-row">
                            <td align="right" valign="top"><i>Født</i></td>
                            <td valign="top">
                                <ul style="padding-left: 15px; margin-top: 0">
                                    <li><xsl:value-of select="//fn:map[@key='born']/fn:string[@key='date']"/></li>
                                    <li><xsl:value-of select="//fn:map[@key='born']/fn:string[@key='place']"/></li>
                                </ul>
                            </td>
                        </tr>
                        <tr class="author-row">
                            <td align="right" valign="top"><i>Død</i></td>
                            <td valign="top">
                                <ul style="padding-left: 15px; margin-top: 0">
                                    <li><xsl:value-of select="//fn:map[@key='dead']/fn:string[@key='date']"/></li>
                                    <li><xsl:value-of select="//fn:map[@key='dead']/fn:string[@key='place']"/></li>
                                </ul>
                            </td>
                        </tr>
                    </table>
                  </div>

                   <div class="toc">
                    <table border="0" cellspacing="0" cellpadding="5" width="300" align="left">
                        <tr class="author-row">
                            <th colspan="2" align="center" valign="middle">TOC</th>
                        </tr>
                        <tr class="toc-row">
                            <td align="right" valign="top"><i>Vers</i></td>
                            <td>
                                <ol  style="padding-left: 15px; margin-top: 0">
                                    <xsl:for-each select="//fn:map[@key='text']/fn:array[@key='content_html']/fn:array">
                                        <xsl:if test="fn:map/fn:number[@key='num'] = '1' or preceding-sibling::*[position()=1]/fn:string = ''">
                                            <li>
                                                <a>
                                                    <xsl:attribute name="href">#<xsl:value-of select="position()"/></xsl:attribute>
                                                    <xsl:value-of disable-output-escaping="yes" select="fn:string"/>
                                                </a>
                                            </li>
                                        </xsl:if>
                                        <xsl:text>&#10;</xsl:text>
                                    </xsl:for-each>
                                </ol>
                            </td>
                        </tr>
                    </table>
                   </div>
                </div>



                <xsl:for-each select="//fn:map[@key='text']/fn:array[@key='content_html']/fn:array">
                    <xsl:if test="fn:map/fn:number[@key='num'] = '1' or preceding-sibling::*[position()=1]/fn:string = ''">
                        <a>
                            <xsl:attribute name="name">
                                <xsl:value-of select="position()"/>
                            </xsl:attribute>
                        </a>
                    </xsl:if>
                    <xsl:value-of disable-output-escaping="yes" select="fn:string"/><br />
                    <xsl:text>&#10;</xsl:text>
                </xsl:for-each>
            </body>
        </html>
    </xsl:template>
</xsl:stylesheet>
