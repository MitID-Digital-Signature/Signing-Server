<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                version="3.0">
    <xsl:output method="html" indent="no"/>
    <xsl:param name="useMonoSpaceFont" select="'false'"/>
    <xsl:template match="/">
        <html lang="da">
            <body>
                <xsl:choose>
                    <xsl:when test="$useMonoSpaceFont = 'true'">
                        <pre><xsl:for-each select="/data/line"><xsl:value-of select="."/><br/></xsl:for-each></pre>
                    </xsl:when>
                    <xsl:otherwise>
                        <div><xsl:for-each select="/data/line"><xsl:value-of select="."/><br/></xsl:for-each></div>
                    </xsl:otherwise>
                </xsl:choose>
            </body>
        </html>
    </xsl:template>
</xsl:stylesheet>
