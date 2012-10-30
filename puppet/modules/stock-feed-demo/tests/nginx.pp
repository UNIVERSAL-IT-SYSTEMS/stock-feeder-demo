Exec { path => [ "/bin/", "/sbin/" , "/usr/bin/", "/usr/sbin/" ] }

package { "lighttpd-remove":
	name => "lighttpd",
	ensure => "purged",
}

class { "stock-feed-demo":
}

stock-feed-demo::nginx { "stock-feed-demo":
	require => [Class["stock-feed-demo"], Package["lighttpd-remove"]  ]
}