define stock-feed-demo::nginx {
	exec { "add-apt-repository ppa:nginx/stable":
		unless => "grep -R nginx /etc/apt/* 2>/dev/null"
	}

	exec { "apt-get update"
		require => Exec["add-apt-repository -y ppa:nginx/stable"]
	}

	package { "nginx":
		ensure => latest,
		require => Exec["apt-get update"]
	}
	
	service { "nginx":
		enable => true,
	    ensure => running,
	    require => Package[nginx],
	}
	
	file { "/etc/nginx/sites-available/stock-feed-demo":
		owner   => "root",
		group   => "root",
		mode    => "0644",
		source  => "puppet:///modules/stock-feed-demo/etc/nginx/sites-available/stock-feed-demo",
		require => Package["nginx"],
	}
	
	file { "/etc/nginx/sites-enabled/stock-feed-demo":
		ensure  => link,
		target => "/etc/nginx/sites-available/stock-feed-demo",
		require => File["/etc/nginx/sites-available/stock-feed-demo"]
		notify  => Service["nginx"],
	}	

	file { "/etc/nginx/sites-enabled/default":
		ensure  => absent
	}	
}