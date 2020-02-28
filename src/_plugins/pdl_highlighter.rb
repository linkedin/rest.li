# This "hook" is executed right before the site's pages are rendered
Jekyll::Hooks.register :site, :pre_render do |site|
  puts "Registering PDL lexer..."
  require "rouge"

  # This class defines the PDL lexer which is used to highlight "pdl" code snippets during render-time
  class PdlLexer < Rouge::RegexLexer
    title 'PDL'
    desc 'Pegasus Data Language (rest.li)'
    tag 'pdl'  # This function call registers this lexer with the global Rouge language resolver, allowing it to be used during render-time 
    filenames '*.pdl'

    primitives = %w(int long float double bytes string null boolean)

    id = /\w+|`\w+`/
    fqid = /(?:\w+|`\w+`)(?:\.(?:\w+|`\w+`))*/
    property_key = /(?:\w+|`[\w\.]+`)(?:\.(?:\w+|`[\w\.]+`))*/

    # At the outset of lexing, expect a scoped type declaration
    start do
      push :scoped_type
    end

    # This is useless because of the above statement (I think...)
    state :root do
      mixin :whitespace
    end

    ### Whitespace, comments, and properties are "mixins" which are pretty much allowed everywhere indiscriminately

    state :mixins do
      mixin :whitespace
      mixin :comment
      mixin :property
    end

    state :whitespace do
      rule %r/[\s,]/, Punctuation
    end

    state :comment do
      rule %r(//.*?$), Comment::Single
      rule %r(/\*.*?\*/)m, Comment::Multiline
      rule %r(/\*\*.*?\*/)m, Comment::Multiline
    end

    state :property do
      rule %r/(@#{property_key})(\s*)(=)/ do
        groups Name::Decorator, Text, Punctuation
        push :json_root
      end
      rule %r/@#{property_key}/, Name::Decorator
    end

    ### Type Declarations and References

    # Scoped type declaration, which wraps a normal type assignment
    state :scoped_type do
      mixin :mixins

      rule %r/(namespace|package|import)(\s+)(#{fqid})/ do
        groups Keyword::Reserved, Text, Name
      end

      rule %r/}/, Punctuation, :pop!

      rule %r//, Text, :type
    end

    # The main entry point for type assignments (i.e. declarations and references)
    state :type do
      mixin :mixins

      # Check for primitives
      rule %r/(?:#{primitives.join('|')})\b/, Keyword::Type, :pop!

      # Check for named type declarations
      rule %r/record\b/ do
        token Keyword::Reserved
        goto :record
      end
      rule %r/enum\b/ do
        token Keyword::Reserved
        goto :enum
      end
      rule %r/typeref\b/ do
        token Keyword::Reserved
        goto :typeref
      end
      rule %r/fixed\b/ do
        token Keyword::Reserved
        goto :fixed
      end

      # Check for anonymous type declarations
      rule %r/union\b/ do
        token Keyword::Reserved
        goto :union
      end
      rule %r/array\b/ do
        token Keyword::Reserved
        goto :array
      end
      rule %r/map\b/ do
        token Keyword::Reserved
        goto :map
      end

      # Check for scoped type declarations
      rule %r/{/ do
        token Punctuation
        goto :scoped_type
      end

      # If nothing else, must be a type reference
      rule %r/#{fqid}/, Name, :pop!
    end

    ### Named Type Declarations

    state :record do
      mixin :mixins

      rule %r/includes\b/, Keyword::Reserved, :includes

      # Check for a field declaration
      rule %r/(#{id})(\s*)(:)(\s*)(optional)?/ do
        groups Name::Label, Text, Punctuation, Text, Keyword::Reserved
        push :type
      end
      rule %r/=/, Punctuation, :json_root
      rule %r/#{id}/, Name
      rule %r/{/, Punctuation
      rule %r/}/, Punctuation, :pop!
    end

    # This is an auxiliary state used above by the "record" state
    state :includes do
      mixin :mixins

      # Open brace surely means that the includes statements are done
      rule %r/{/, Punctuation, :pop!

      # Else, force lex a type declaration
      rule %r//, Text, :type
    end

    state :enum do
      mixin :mixins

      rule %r/#{id}/, Name
      rule %r/{/, Punctuation
      rule %r/}/, Punctuation, :pop!
    end

    state :typeref do
      mixin :mixins

      rule %r/#{id}/, Name
      rule %r/=/ do
        token Punctuation
        goto :type
      end
    end

    state :fixed do
      mixin :mixins

      rule %r/\d+/, Num::Integer
      rule %r/#{id}/, Name
    end

    ### Anonymous Type Declarations

    state :union do
      mixin :mixins

      rule %r/\[/, Punctuation
      rule %r/\]/, Punctuation, :pop!

      # Aliased union member
      rule %r/(#{id})(\s*)(:)/ do
        groups Name::Label, Text, Punctuation
        push :type
      end

      # If no other rule was matched, force the lexer to parse a type assignment
      rule %r//, Text, :type
    end

    state :array do
      mixin :mixins

      rule %r/\[/, Punctuation, :type
      rule %r/\]/, Punctuation, :pop!
    end

    state :map do
      mixin :mixins

      rule %r/\[/ do
        token Punctuation
        push :type
        push :type
      end

      rule %r/\]/, Punctuation, :pop!
    end

    ### JSON States
    # TODO: Perhaps this can be done better using the "delegate" function

    state :json_whitespace do
      rule %r/\s+/, Text::Whitespace
    end

    state :json_root do
      mixin :json_whitespace

      # Constants
      rule %r/(?:true|false|null)/, Keyword::Constant, :pop!
      rule %r/-?(?:0|[1-9]\d*)\.\d+(?:e[+-]?\d+)?/i, Num::Float, :pop!
      rule %r/-?(?:0|[1-9]\d*)(?:e[+-]?\d+)?/i, Num::Integer, :pop!

      # Non-constants
      rule %r/"/ do
        token Str::Double
        goto :json_string
      end
      rule %r/\[/ do
        token Punctuation
        goto :json_array
      end
      rule %r/{/ do
        token Punctuation
        goto :json_object
      end
    end

    state :json_object do
      mixin :json_whitespace
      mixin :json_name
      mixin :json_value
      rule %r/}/, Punctuation, :pop!
      rule %r/,/, Punctuation
    end

    state :json_name do
      rule %r/("(?:\\.|[^"\\\n])*?")(\s*)(:)/ do
        groups Name::Label, Text::Whitespace, Punctuation
      end
    end

    state :json_value do
      mixin :json_whitespace
      mixin :json_constants
      rule %r/"/, Str::Double, :json_string
      rule %r/\[/, Punctuation, :json_array
      rule %r/{/, Punctuation, :json_object
    end

    state :json_string do
      rule %r/[^\\"]+/, Str::Double
      rule %r/\\./, Str::Escape
      rule %r/"/, Str::Double, :pop!
    end

    state :json_array do
      mixin :json_value
      rule %r/\]/, Punctuation, :pop!
      rule %r/,/, Punctuation
    end

    state :json_constants do
      rule %r/(?:true|false|null)/, Keyword::Constant
      rule %r/-?(?:0|[1-9]\d*)\.\d+(?:e[+-]?\d+)?/i, Num::Float
      rule %r/-?(?:0|[1-9]\d*)(?:e[+-]?\d+)?/i, Num::Integer
    end
  end
end