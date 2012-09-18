import json
import re

from flask import Flask, abort, request, url_for
from pymongo import Connection


app = Flask(__name__)
app.db = Connection('localhost', 27017).member_database


def jsonify(obj, **kwargs):
    return app.response_class(
        json.dumps(obj, indent=None if request.is_xhr else 2),
        mimetype='application/json',
        **kwargs)


@app.route('/members/<name>/', methods=('GET',))
def member_by_name(name):
    member = app.db.members.find_one({'name':name}, {'_id': 0})
    if member is None:
        abort(404)
    return jsonify(member)


def handle_multi_get():
    names = request.args.getlist('name', unicode)
    if not names:
        abort(404)

    members = list(app.db.members.find({'name': {'$in': names}}, {'_id': 0}))
    if not members:
        abort(404)

    return jsonify(members)


def handle_find():
    bio = request.args.get('bio')
    if bio:
        bio_re = re.compile(bio, flags=re.I)
        members = list(app.db.members.find({'bio': bio_re}, {'_id': 0}))
    else:
        members = list(app.db.members.find({}, {'_id': 0}))

    if not members:
        abort(404)

    total_count = len(members)
    start = request.args.get('start', type=int)
    count = request.args.get('count', type=int)
    if start >= 0 and count > 0:
        end = start + count
        members = members[start:end]
        links = {}
        if start > 0:
            links['prev'] = url_for('members', bio=bio, method='find',
                                    count=count,
                                    start=(start-count if start-count > 0 else 0),
                                    _external=True)
        if start + count < total_count:
            links['next'] = url_for('members', bio=bio, method='find',
                                    start=start+count,
                                    count=(count if start+count+count < total_count
                                           else total_count-start+count),
                                    _external=True)
        return jsonify(dict(links, total_count=total_count, items=members))
    else:
        return jsonify(members)


@app.route('/members/', methods=('GET',))
def members():
    # Handle batch get
    if request.args.get('multi') == 'true':
        return handle_multi_get()

    # Handle find use case
    return handle_find()

@app.route('/members/', methods=('POST',))
def create_member():
    # First, try to dispatch a method. Ugly, but whatever.
    method = request.args.get('method')
    if method:
        method = 'post_' + method
        fn = globals().get(method)
        if not fn:
            abort(404)
        return fn()

    multi = request.args.get('multi') == 'true'
    member_json = request.data
    if multi:
        members = json.loads(member_json)
    else:
        members = [json.loads(member_json)]

    for member in members:
        if 'name' not in member or 'bio' not in member:
            abort(400)

    member_ids = app.db.members.insert(members)
    if not member_ids:
        abort(500)

    if multi:
        body = [url_for('member_by_name', name=member['name'],
                        _external=True) for member in members]
    else:
        body = url_for('member_by_name', name=members[0]['name'],
                       _external=True)

    return jsonify(body, status=201)


@app.route('/members/<name>/', methods=('DELETE',))
def delete_member(name):
    err = app.db.members.remove({'name':name}, safe=True)
    if err['err']:
        abort(500)
    if not err['n']:
        abort(404)

    return jsonify({'status':'deleted'})


@app.route('/members/', methods=('DELETE',))
def delete_members():
    names = request.args.getlist('name', type=str)
    if not names:
        abort(400)

    err = app.db.members.remove({'name':{'$in': names}}, safe=True)
    if err['err']:
        abort(500)
    if not err['n']:
        abort(404)

    return jsonify({'status':'deleted'})


@app.route('/members/mostFriendly/', methods=('GET', ))
def most_friendly():
    member = app.db.members.find_one({'name':'rama'}, {'_id': 0})
    if member is None:
        abort(404)
    return jsonify(member)


def post_createFromBooks():
    createReq = json.loads(request.data)
    firstName = createReq.get('firstName')
    lastName = createReq.get('lastName')
    books = createReq.get('books')

    if not firstName or not lastName or not books:
        abort(400)

    member = {
        'name': firstName + ' ' + lastName,
        'bio': firstName + ' wrote ' + ', '.join(books)
    }

    member_id = app.db.members.insert(member)
    if not member_id:
        abort(500)
    return jsonify(url_for('member_by_name', name=member['name'],
                           _external=True), status=201)


if __name__ == '__main__':
    app.run(host='0.0.0.0', debug=True)
