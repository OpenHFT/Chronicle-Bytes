#!/usr/bin/env ruby
# Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0

require 'asciidoctor'
require 'cgi'
require 'fileutils'
require 'pathname'
require 'uri'

root = File.expand_path('..', __dir__)
output = File.join(root, 'target/docs')
sources = ['README.adoc', 'docs/systemProperties.adoc', 'microbenchmarks/README.adoc'] +
          Dir.glob(File.join(root, 'src/main/docs/*.adoc')).map { |path| Pathname.new(path).relative_path_from(Pathname.new(root)).to_s }
rendered = {}
sources.each do |source|
  path = File.join(root, source)
  # Syntax highlighting is optional; link validation needs no extra gems or network access.
  rendered[path] = Asciidoctor.convert_file(path, safe: :safe, to_file: false, header_footer: true,
                                          attributes: { 'source-highlighter!' => '' })
end

errors = []
assets = []
links_checked = 0
rendered.each do |source, html|
  html.scan(/(?:href|src)="([^"]+)"/).flatten.uniq.each do |encoded|
    url = CGI.unescapeHTML(encoded)
    next if url.match?(/\A(?:[a-z][a-z0-9+.-]*:|\/\/)/i)

    path, fragment = url.split('#', 2)
    target = path.empty? ? source : File.expand_path(URI::DEFAULT_PARSER.unescape(path), File.dirname(source))
    target = target.sub(/\.html\z/, '.adoc') if !File.file?(target) && target.end_with?('.html')
    links_checked += 1
    unless target.start_with?(root + '/') && File.file?(target)
      errors << "#{source.delete_prefix(root + '/')}: missing local target #{url}"
      next
    end
    if rendered.key?(target)
      ids = rendered[target].scan(/\bid="([^"]+)"/).flatten
      if fragment && !fragment.empty? && !ids.include?(URI::DEFAULT_PARSER.unescape(fragment))
        errors << "#{source.delete_prefix(root + '/')}: missing anchor #{url}"
      end
    else
      assets << target
    end
  end
end

unless errors.empty?
  warn errors.join("\n")
  abort "Documentation check failed: #{errors.length} broken local links or images"
end

rendered.each do |source, html|
  target = File.join(output, source.delete_prefix(root + '/').sub(/\.adoc\z/, '.html'))
  FileUtils.mkdir_p(File.dirname(target))
  File.write(target, html)
end
assets.uniq.each do |source|
  target = File.join(output, source.delete_prefix(root + '/'))
  FileUtils.mkdir_p(File.dirname(target))
  FileUtils.cp(source, target)
end
puts "Checked #{sources.length} documents and #{links_checked} local links/images; rendered to #{output}"
puts 'External URLs and Mermaid diagrams require separate review; they are not validated by this check.'
