
class stock-feeder-demo {
	package { "lighttpd":
		ensure => latest
	}
	
	service { "lighttpd":
	        enable => true,
	        ensure => running,
	        require => Package[lighttpd],
	}
	
	file { "/etc/lighttpd/lighttpd.conf":
		owner   => "root",
		group   => "root",
		mode    => "0644",
		source  => "puppet:///modules/stock_feeder_demo/etc/lighttpd/lighttpd.conf",
		require => Package["lighttpd"],
		notify  => Service["lighttpd"],
	}
	
	file { "/var/www/index.lighttpd.html":
		ensure  => absent
	}	

	file { "/var/www/default":
		owner   => "www-data",
		group   => "www-data",
		mode    => "0644",
		recurse => true,
		purge 	=> true,
		force 	=> true,
		source  => "puppet:///modules/stock_feeder_demo/var/www/default",
		require => Package["lighttpd"],
		notify  => Service["lighttpd"],
	}
}
