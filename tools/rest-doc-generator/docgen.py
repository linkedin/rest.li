import json
import optparse
import os

from flask import Flask, render_template, abort, redirect, make_response
from watchdog.events import FileSystemEventHandler
from watchdog.observers import Observer

__author__ = 'rrangana'

app = Flask(__name__)
app.secret_key = "MY SECRET KEY!!!!"

DOC_TEMPLATE = "doc.html"
INDEX_TEMPLATE = "index.html"
MODEL_DOC_TEMPLATE = "model_doc.html"

@app.route('/specs/<spec_id>/')
def read(spec_id):
    spec = get_specs(REST_SPECS).get(spec_id)
    if not spec:
        abort(404)
    root_spec = json.loads(spec['schema'])
    return render_template(DOC_TEMPLATE, spec_id=spec_id, rootSpec=root_spec)


@app.route('/specs/<spec_id>/json/')
def read_json(spec_id):
    spec = get_specs(REST_SPECS).get(spec_id)
    if not spec:
        abort(404)
    r = make_response(spec['schema'])
    r.headers['Content-Type'] = 'application/json'
    return r

@app.route('/models/<model_id>/')
def read_model(model_id):
    model = get_specs(MODELS).get(model_id)
    if not model:
        abort(404)
    model = json.loads(model['schema'])
    return render_template(MODEL_DOC_TEMPLATE, model_id=model_id, model=model)


@app.route('/models/<model_id>/json/')
def read_model_json(model_id):
    model = get_specs(MODELS).get(model_id)
    if not model:
        abort(404)
    r = make_response(model['schema'])
    r.headers['Content-Type'] = 'application/json'
    return r


@app.route('/')
def index():
    rest_specs = get_specs(REST_SPECS)
    models = get_specs(MODELS)
    return render_template(INDEX_TEMPLATE,
                           rest_specs=[json.loads(spec['schema']) for spec in rest_specs.values()],
                           models=[json.loads(model['schema']) for model in models.values()])

@app.route('/models/')
def models_index():
    models = get_specs(MODELS)
    return render_template(INDEX_TEMPLATE,
                           rest_specs=None,
                           models=[json.loads(model['schema']) for model in models.values()])


@app.route('/specs/')
def specs_index():
    rest_specs = get_specs(REST_SPECS)
    return render_template(INDEX_TEMPLATE,
                           rest_specs=[json.loads(spec['schema']) for spec in rest_specs.values()],
                           models=None)


def instanceof(s, t):
    if isinstance(t, basestring):
        return isinstance(s, basestring)
    return isinstance(s, type(t))
app.jinja_env.tests['instanceof'] = instanceof


# TODO: Synchronize access to this. Atomic operations are ok, but most reads aren't atomic operations
SCHEMAS = {}
def load_path(path):
    SCHEMAS[path] = {}  # clear out existing specs
    rest_specs = {}
    models = {}
    for root, dirs, files in os.walk(path):
        for file in files:
            filename = os.path.join(root, file)
            if file.endswith(('.json', '.pdsc')):
                schemas = rest_specs if file.endswith('.json') else models

                f = open(filename, 'r')
                text = f.read()
                schema = json.loads(text)
                # TODO: Handle aliases
                schemas[schema['name']] = {
                    'name': schema['name'],
                    'filename': filename,
                    'schema': text
                }
                f.close()
            elif file.endswith('.avpr'):
                schemas = models
                f = open(filename, 'r')
                protocol = json.load(f)
                for schema in protocol.get('types'):
                    schemas[schema['name']] = {
                        'name': schema['name'],
                        'filename': filename,
                        'schema': json.dumps(schema, indent=1)
                    }
                f.close()

    SCHEMAS[path][MODELS] = models
    SCHEMAS[path][REST_SPECS] = rest_specs

REST_SPECS = 'rest_specs'
MODELS = 'models'

def get_specs(type):
    """
    Returns all specs of the given type as a dictionary name -> spec. A spec is a dictionary containing the
    name, the filename, and the json content of a given schema.
    """
    specs = {}
    for schema_dir in SCHEMAS:
        sub_specs = SCHEMAS[schema_dir].get(type)
        if sub_specs:
            specs.update(sub_specs)
    return specs


class SpecEventHandler(FileSystemEventHandler):
    def __init__(self, path):
        self.path = path

    def on_any_event(self, event):
        # horrifically inefficient, but whatever
        print 'Reloading ' + self.path
        load_path(self.path)


if __name__ == '__main__':
    parser = optparse.OptionParser()
    parser.add_option('--paths', action='store', default='.',
                      help='Comma-separated list of paths that have the schema files')
    options, args = parser.parse_args()
    observers = []
    for path in options.paths.split(','):
        load_path(path)
        observer = Observer()
        observer.schedule(SpecEventHandler(path), path=path, recursive=True)
        observer.start()
        observers.append(observer)

    app.run(host='0.0.0.0', debug=True, port=5001)

    for observer in observers:
        observer.stop()
        observer.join()
