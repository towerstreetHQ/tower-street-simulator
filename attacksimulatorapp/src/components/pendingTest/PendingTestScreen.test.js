import { uniqueTests } from './PendingTestScreen';

describe('uniqueTests', () => {
    it('returns same array if there are no duplicates', () => {
        const arr = [
            { label: 'foo'}, 
            { label: 'bar'}, 
            { label: 'baz'}
        ]
        expect(uniqueTests(arr)).toEqual(arr)
    })

    it('returns same array if duplicates do not follow each other', () => {
        const arr = [
            { label: 'foo' },
            { label: 'bar' },
            { label: 'baz' },
            { label: 'foo' }
        ]
        expect(uniqueTests(arr)).toEqual(arr)
    })

    it('filters duplicates', () => {
        const input = [
            { label: 'foo' },
            { label: 'foo' },
            { label: 'baz' }
        ]
        const output = [
            { label: 'foo' },
            { label: 'baz' }
        ]
        expect(uniqueTests(input)).toEqual(output)
    })
})