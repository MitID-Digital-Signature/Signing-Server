# Default server configuration
#
server {

	root /var/www/example.vhost.com;

	# Add index.php to the list if you are using PHP
	index index.html index.php;
	server_name example.vhost.com;

	rewrite ^/test/?$ /test/signtest.php permanent;

	location = /sign/get {
            proxy_set_header X-Forwarded-Host $host:$server_port;
            proxy_set_header X-Forwarded-Server $host;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_pass   http://127.0.0.1:8088/index.html;
	}

	location ~ ^/(sign/|signing-result|signing-cancel|css/|webjars/) {
            proxy_set_header X-Forwarded-Host $host:$server_port;
            proxy_set_header X-Forwarded-Server $host;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_pass http://127.0.0.1:8088;
	}

	charset utf8;
 	error_page  404 /404.html;

	# pass PHP scripts to FastCGI server

	location ~ \.php$ {
		include snippets/fastcgi-php.conf;
		fastcgi_pass unix:/run/php/php7.4-fpm.sock;
	}

	location ~ /\.(ht|ini|log|conf) {
		deny all;
		error_page 403 =404 / ;
	}

    listen 443 ssl; # managed by Certbot
    ssl_certificate /etc/letsencrypt/live/example.vhost.com/fullchain.pem; # managed by Certbot
    ssl_certificate_key /etc/letsencrypt/live/example.vhost.com/privkey.pem; # managed by Certbot
    include /etc/letsencrypt/options-ssl-nginx.conf; # managed by Certbot
    ssl_dhparam /etc/letsencrypt/ssl-dhparams.pem; # managed by Certbot

}

server {
    if ($host = example.vhost.com) {
        return 301 https://$host$request_uri;
    } # managed by Certbot

        listen 80;
	server_name example.vhost.com;
    return 404; # managed by Certbot


}
